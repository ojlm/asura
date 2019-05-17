package asura.core.runtime

import java.util

import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{Environment, VariablesExportItem, VariablesImportItem}
import asura.core.es.service.EnvironmentService
import asura.core.script.JavaScriptEngine
import asura.core.util.{JsonPathUtils, StringTemplate}

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
  // alias for all steps
  val KEY__C = "_c"
  // alias for prev step
  val KEY__P = "_p"
  // current step status, now only http step
  val KEY_STATUS = "status"
  // current step headers, now only http step
  val KEY_HEADERS = "headers"
  // current step response body, eg: http, dubbo, sql
  val KEY_ENTITY = "entity"
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
    * @param template template string which must start with "${" and end with "}"
    * @param ctx      context must be java types
    * @return template itself or value in context wrapped in a future
    */
  def render(template: String, ctx: util.Map[Any, Any]): Any = {
    if (StringUtils.isNotEmpty(template)) {
      if (template.startsWith(TEMPLATE_PREFIX) && template.endsWith(TEMPLATE_SUFFIX)) {
        val tplMacro = template.substring(TEMPLATE_PREFIX.length, template.length - TEMPLATE_SUFFIX.length)
        if (StringUtils.isNotEmpty(tplMacro)) {
          if (tplMacro.startsWith(JSON_PATH_MACRO_PREFIX_1) || tplMacro.startsWith(JSON_PATH_MACRO_PREFIX_2)) {
            JsonPathUtils.read[Any](ctx, tplMacro)
          } else {
            val bindings = new java.util.HashMap[String, Any]()
            bindings.put(SELF_VARIABLE, ctx)
            JavaScriptEngine.eval(tplMacro, bindings)
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
    * render whole body to string which may have many macros.
    * this will throw exception when json-path is not found
    */
  def renderBody(template: String, ctx: util.Map[Any, Any]): String = {
    if (StringUtils.isNotEmpty(template)) {
      StringTemplate.mustacheParser.parse(template, macroName => {
        if (macroName.startsWith(JSON_PATH_MACRO_PREFIX_1) || macroName.startsWith(JSON_PATH_MACRO_PREFIX_2)) {
          JsonPathUtils.read[Any](ctx, macroName).toString
        } else {
          val bindings = new java.util.HashMap[String, Any]()
          bindings.put(SELF_VARIABLE, ctx)
          JavaScriptEngine.eval(macroName, bindings).toString
        }
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

  def setPrevContext(prevContext: util.Map[Any, Any]): RuntimeContext = {
    if (null != prevContext && !prevContext.isEmpty) {
      ctx.put(RuntimeContext.KEY__P, prevContext)
      val cases = ctx.get(RuntimeContext.KEY__C)
      if (null != cases) {
        cases.asInstanceOf[util.ArrayList[util.Map[Any, Any]]].add(prevContext)
      } else {
        val list = new util.ArrayList[util.Map[Any, Any]]()
        list.add(prevContext)
        ctx.put(RuntimeContext.KEY__C, list)
      }
    }
    this
  }

  def evaluateImportsVariables(imports: Seq[VariablesImportItem]): RuntimeContext = {
    if (null != imports && imports.nonEmpty) {
      imports.filter(item => null != item && item.isValid()).foreach(item => {
        // usually do not need to check scope can only be one of `_g`, `_j`, `_s`
        var scopeCtx = ctx.get(item.scope)
        if (null == scopeCtx) {
          // use a HashMap to allow null values, currently the step run in sequence one by one
          // the should be no thread-safe problem
          scopeCtx = new util.HashMap[Any, Any]()
          ctx.put(item.scope, scopeCtx)
        }
        scopeCtx.asInstanceOf[util.Map[Any, Any]].put(item.name, item.value)
      })
    }
    this
  }

  def evaluateExportsVariables(exports: Seq[VariablesExportItem]): RuntimeContext = {
    if (null != exports && exports.nonEmpty) {
      exports.filter(item => null != item && item.isValid()).foreach(item => {
        var value: Object = null
        try {
          value = JsonPathUtils.read[Object](ctx, item.srcPath)
        } catch {
          case t: Throwable =>
            value = t.getMessage
        }
        // usually do not need to check scope can only be one of `_g`, `_j`, `_s`
        var scopeCtx = ctx.get(item.scope)
        if (null == scopeCtx) {
          // use a HashMap to allow null values
          scopeCtx = new util.HashMap[Any, Any]()
          ctx.put(item.scope, scopeCtx)
        }
        scopeCtx.asInstanceOf[util.Map[Any, Any]].put(item.dstName, value)
      })
    }
    this
  }

  def eraseCurrentData(): RuntimeContext = {
    ctx.remove(RuntimeContext.KEY_STATUS)
    ctx.remove(RuntimeContext.KEY_HEADERS)
    ctx.remove(RuntimeContext.KEY_ENTITY)
    this
  }

  def setCurrentStatus(status: Int): RuntimeContext = {
    ctx.put(RuntimeContext.KEY_STATUS, status)
    this
  }

  def setCurrentHeaders(headers: java.util.HashMap[String, String]): RuntimeContext = {
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

  def renderSingleMacroAsString(tpl: String): String = {
    RuntimeContext.render(tpl, ctx).toString
  }

  def renderBodyAsString(tpl: String): String = {
    RuntimeContext.renderBody(tpl, ctx).toString
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
}
