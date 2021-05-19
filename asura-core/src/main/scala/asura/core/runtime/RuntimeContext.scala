package asura.core.runtime

import java.util

import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{Environment, VariablesExportItem, VariablesImportItem, VariablesItemExtraData}
import asura.core.es.service.EnvironmentService
import asura.core.script.JsEngine
import asura.core.script.function.{ArgWithExtraData, Functions}
import asura.core.util.{JsonPathUtils, StringTemplate}

import scala.collection.mutable
import scala.concurrent.Future

object RuntimeContext {

  // ###########################
  // # builtin keys in context #
  // ###########################

  // alias for global scope
  val KEY__G = "_g"
  // alias for job scope
  val KEY__J = "_j"
  // alias for scenario scope
  val KEY__S = "_s"
  // current step status, now only http step
  val KEY_STATUS = "status"
  // current step headers, now only http step
  val KEY_HEADERS = "headers"
  // current step response body, eg: http, dubbo, sql
  val KEY_ENTITY = "entity"
  // current step time metrics
  val KEY_TIME = "time"
  // same with `asura.core.es.model.JobReportData.JobReportStepItemMetrics.requestTime`
  val KEY_REQUEST = "request"
  // current step request, only http step
  val KEY__REQ = "_req"
  // alias for variable table of environment
  val KEY__ENV = "_env"

  val TEMPLATE_PREFIX = "{{"
  val TEMPLATE_SUFFIX = "}}"
  val JSON_PATH_MACRO_PREFIX_1 = "$."
  val JSON_PATH_MACRO_PREFIX_2 = "$["
  val SELF_VARIABLE = "$"

  /**
   * render single template macro which can generate field of case.
   * this will throw exception when json-path is not found
   *
   * e.g:
   *
   * "" => ""
   * "abc" => "abc"
   * "{{$.abc}}" => return context.abc
   * "{{random(3)}}" => return script evaluate result
   * "{{}}" => ""
   *
   * @param template template string which must start with `${` and end with `}`
   * @param ctx      context must be java types
   * @return template itself or value in context wrapped in a future
   * */
  def renderSingleMacro(template: String, ctx: util.Map[Any, Any]): Any = {
    if (StringUtils.isNotEmpty(template)) {
      if (template.startsWith(TEMPLATE_PREFIX) && template.endsWith(TEMPLATE_SUFFIX)) {
        val tplMacro = template.substring(TEMPLATE_PREFIX.length, template.length - TEMPLATE_SUFFIX.length)
        if (StringUtils.isNotEmpty(tplMacro)) {
          if (tplMacro.startsWith(JSON_PATH_MACRO_PREFIX_1) || tplMacro.startsWith(JSON_PATH_MACRO_PREFIX_2)) {
            JsonPathUtils.read[Any](ctx, tplMacro)
          } else {
            val bindings = new java.util.HashMap[String, Any]()
            bindings.put(SELF_VARIABLE, ctx)
            JsEngine.eval(tplMacro, bindings)
          }
        } else {
          StringUtils.EMPTY
        }
      } else {
        template
      }
    } else {
      StringUtils.EMPTY
    }
  }

  /**
   * render whole template to string which may have many macros.
   * this will throw exception when json-path is not found
   */
  def renderTemplate(template: String, ctx: util.Map[Any, Any]): String = {
    if (StringUtils.isNotEmpty(template)) {
      StringTemplate.mustacheParser.parse(template, macroName => {
        val bodyString = if (macroName.startsWith(JSON_PATH_MACRO_PREFIX_1) || macroName.startsWith(JSON_PATH_MACRO_PREFIX_2)) {
          JsonPathUtils.read[Any](ctx, macroName)
        } else {
          val bindings = new java.util.HashMap[String, Any]()
          bindings.put(SELF_VARIABLE, ctx)
          JsEngine.eval(macroName, bindings)
        }
        if (null != bodyString) bodyString.toString else "null"
      })
    } else {
      StringUtils.EMPTY
    }
  }

  def apply(): RuntimeContext = new RuntimeContext()

  def apply(rawContext: util.Map[Any, Any]) = new RuntimeContext(rawContext)

  // extract current report result specified context as a new thread-unsafe map
  def extractSelfContext(result: AbstractResult): util.Map[Any, Any] = {
    val context = result.context
    val selfContext = new util.HashMap[Any, Any]()
    if (null != context && !context.isEmpty) {
      val status = context.get(RuntimeContext.KEY_STATUS)
      if (Option(status).isDefined) selfContext.put(RuntimeContext.KEY_STATUS, status)
      val headers = context.get(RuntimeContext.KEY_HEADERS)
      if (null != headers) selfContext.put(RuntimeContext.KEY_HEADERS, headers)
      val entity = context.get(RuntimeContext.KEY_ENTITY)
      if (null != entity) selfContext.put(RuntimeContext.KEY_ENTITY, entity)
    }
    selfContext
  }
}

/**
 * use java type system
 */
case class RuntimeContext(
                           private val ctx: util.Map[Any, Any] = new util.concurrent.ConcurrentHashMap[Any, Any](),
                           var options: ContextOptions = null,
                         ) {

  def rawContext = ctx

  def evaluateImportsVariables(imports: Seq[VariablesImportItem]): Future[RuntimeContext] = {
    if (null != imports && imports.nonEmpty) {
      val items = imports.filter(item => null != item && item.isValid())
      items.foldLeft(Future.successful(this))((futureRc, item) => {
        for {
          _ <- futureRc
          next <- {
            val futureValue = if (null != item.value) {
              val value = if (VariablesImportItem.TYPE_ENUM.equals(item.`type`)
                && null != item.extra && null != item.extra.options
              ) {
                val kvOpt = item.extra.options.find(kv => item.value.equals(kv.key))
                if (kvOpt.nonEmpty) kvOpt.get.value else item.value
              } else {
                item.value
              }
              if (StringUtils.isNotEmpty(item.function)) {
                evaluateValue(value, item.function, item.extra)
              } else {
                Future.successful(value)
              }
            } else {
              Future.successful(item.value)
            }
            futureValue.map(value => putValueToScope(item.name, value, item.scope))
          }
        } yield next
      })
    } else {
      Future.successful(this)
    }
  }

  def evaluateExportsVariables(exports: Seq[VariablesExportItem]): Future[RuntimeContext] = {
    if (null != exports && exports.nonEmpty) {
      val items = exports.filter(item => null != item && item.isValid())
      items.foldLeft(Future.successful(this))((futureRc, item) => {
        for {
          _ <- futureRc
          next <- {
            val futureValue = try {
              val tmpValue = JsonPathUtils.read[Object](ctx, item.srcPath)
              if (null != tmpValue && StringUtils.isNotEmpty(item.function)) {
                evaluateValue(tmpValue, item.function, item.extra)
              } else {
                Future.successful(tmpValue)
              }
            } catch {
              case t: Throwable => Future.successful(t.getMessage)
            }
            futureValue.map(value => putValueToScope(item.dstName, value, item.scope))
          }
        } yield next
      })
    } else {
      Future.successful(this)
    }
  }

  def renderedExportsDesc(exports: Seq[VariablesExportItem]): Map[Int, String] = {
    if (null != exports && exports.nonEmpty) {
      val map = mutable.Map[Int, String]()
      for (i <- 0 until exports.size) {
        val item = exports(i)
        if (null != item && item.isValid() && StringUtils.isNotEmpty(item.description)) {
          map(i) = renderTemplateAsString(item.description)
        }
      }
      map.toMap
    } else {
      Map.empty
    }
  }

  private def evaluateValue(value: Object, function: String, extra: VariablesItemExtraData): Future[Object] = {
    val func = Functions.getTransform(function)
    if (func.nonEmpty) {
      val arg = if (null != extra) ArgWithExtraData(value, extra) else value
      func.get.apply(arg).recover {
        case t: Throwable => t.getMessage
      }
    } else {
      Future.successful(s"Function '${function}' not registered")
    }
  }

  def putValueToScope(key: String, value: Object, scope: String): RuntimeContext = {
    // usually do not need to check scope can only be one of `_g`, `_j`, `_s`
    var scopeCtx = ctx.get(scope)
    if (null == scopeCtx) {
      // use a HashMap to allow null values, currently the step run in sequence one by one
      // the should be no thread-safe problem
      scopeCtx = new util.HashMap[Any, Any]()
      ctx.put(scope, scopeCtx)
    }
    scopeCtx.asInstanceOf[util.Map[Any, Any]].put(key, value)
    this
  }

  def eraseCurrentData(): RuntimeContext = {
    ctx.remove(RuntimeContext.KEY_STATUS)
    ctx.remove(RuntimeContext.KEY_HEADERS)
    ctx.remove(RuntimeContext.KEY_ENTITY)
    ctx.remove(RuntimeContext.KEY__REQ)
    ctx.remove(RuntimeContext.KEY_TIME)
    this
  }

  def eraseScenarioData(): RuntimeContext = {
    val value = ctx.get(RuntimeContext.KEY__S)
    if (null != value && value.isInstanceOf[util.Map[_, _]]) {
      value.asInstanceOf[util.Map[_, _]].clear()
    }
    this
  }

  def setCurrentStatus(status: Int): RuntimeContext = {
    ctx.put(RuntimeContext.KEY_STATUS, status)
    this
  }

  def setCurrentMetrics(metrics: RuntimeMetrics): RuntimeContext = {
    val map = new util.HashMap[String, Int]()
    map.put(RuntimeContext.KEY_REQUEST, metrics.getRequestTime())
    ctx.put(RuntimeContext.KEY_TIME, map)
    this
  }

  def setCurrentHeaders(headers: java.util.ArrayList[java.util.Map[String, String]]): RuntimeContext = {
    ctx.put(RuntimeContext.KEY_HEADERS, headers)
    this
  }

  /**
   * @param entity `String` or `java.util.Map`
   */
  def setCurrentEntity(entity: Any): RuntimeContext = {
    ctx.put(RuntimeContext.KEY_ENTITY, entity)
    this
  }

  def setCurrentRequest(req: java.util.Map[String, Object]) = {
    ctx.put(RuntimeContext.KEY__REQ, req)
  }

  def renderSingleMacroAsString(tpl: String): String = {
    RuntimeContext.renderSingleMacro(tpl, ctx).toString
  }

  def renderTemplateAsString(tpl: String): String = {
    RuntimeContext.renderTemplate(tpl, ctx).toString
  }

  def setOrUpdateEnv(env: Environment): RuntimeContext = {
    if (null != env) {
      var envMap = ctx.get(RuntimeContext.KEY__ENV).asInstanceOf[util.Map[Any, Any]]
      if (null == envMap) {
        envMap = new util.concurrent.ConcurrentHashMap[Any, Any]()
        ctx.put(RuntimeContext.KEY__ENV, envMap)
      }
      if (null != env.custom && env.custom.nonEmpty) {
        env.custom.filter(_.enabled).foreach(kv => {
          envMap.put(kv.key, kv.value)
        })
      }
    }
    this
  }

  def evaluateOptions(): Future[Boolean] = {
    if (null != options) {
      if (null != options.initCtx && !options.initCtx.isEmpty) {
        ctx.putAll(options.initCtx)
      }
      val usedEnvId = options.getUsedEnvId()
      if (null != options.getUsedEnv(usedEnvId)) {
        setOrUpdateEnv(options.getUsedEnv())
        Future.successful(true)
      } else if (StringUtils.isNotEmpty(usedEnvId)) {
        EnvironmentService.getEnvById(usedEnvId).map(env => {
          options.setUsedEnv(usedEnvId, env)
          setOrUpdateEnv(env)
          true
        })
      } else {
        Future.successful(true)
      }
    } else {
      Future.successful(true)
    }
  }

  def getGlobalAndJobMap(): Map[Any, Any] = {
    Map(
      RuntimeContext.KEY__G -> ctx.get(RuntimeContext.KEY__G),
      RuntimeContext.KEY__J -> ctx.get(RuntimeContext.KEY__J)
    )
  }
}
