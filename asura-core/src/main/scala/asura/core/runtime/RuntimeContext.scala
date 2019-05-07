package asura.core.runtime

import java.util

import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.Environment
import asura.core.es.service.EnvironmentService
import asura.core.http.HttpResult
import asura.core.script.JavaScriptEngine
import asura.core.util.{JsonPathUtils, StringTemplate}

import scala.concurrent.Future

object RuntimeContext {

  // builtin keys in context
  val KEY__GLOBAL = "_global" // global scope
  val KEY__G = "_g" // global alias
  val KEY__JOB = "_job" // job scope
  val KEY__J = "_j" // job alias
  val KEY__SCENARIO = "_scenario" // current scenario scope
  val KEY__S = "_s" // scenario alias
  val KEY__CASES = "_cases" // current scenario scope
  val KEY__C = "_c" // cases alias
  val KEY__PREV = "_prev" // current scenario scope
  val KEY__P = "_p" // prev alias
  val KEY_STATUS = "status" // current case status
  val KEY_HEADERS = "headers" // current case headers
  val KEY_ENTITY = "entity" // current case http body
  val KEY__ENV = "_env"

  //
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

  def extractCaseSelfContext(caseResult: HttpResult): util.Map[Any, Any] = {
    val context = caseResult.context
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

  def setOrUpdateGlobal(global: util.Map[Any, Any]): RuntimeContext = {
    if (null != global && !global.isEmpty) {
      val g = ctx.get(RuntimeContext.KEY__GLOBAL)
      if (null == g) {
        ctx.put(RuntimeContext.KEY__GLOBAL, global)
        ctx.put(RuntimeContext.KEY__G, global)
      } else {
        g.asInstanceOf[util.Map[Any, Any]].putAll(global)
      }
    }
    this
  }

  def eraseGlobal(): RuntimeContext = {
    ctx.remove(RuntimeContext.KEY__GLOBAL)
    ctx.remove(RuntimeContext.KEY__G)
    this
  }

  def setOrUpdateJob(job: util.Map[Any, Any]): RuntimeContext = {
    if (null != job && !job.isEmpty) {
      val j = ctx.get(RuntimeContext.KEY__JOB)
      if (null == j) {
        ctx.put(RuntimeContext.KEY__JOB, job)
        ctx.put(RuntimeContext.KEY__J, job)
      } else {
        j.asInstanceOf[util.Map[Any, Any]].putAll(job)
      }
    }
    this
  }

  def eraseJob(): RuntimeContext = {
    ctx.remove(RuntimeContext.KEY__JOB)
    ctx.remove(RuntimeContext.KEY__J)
    this
  }

  def setOrUpdateScenario(scenario: util.Map[Any, Any]): RuntimeContext = {
    if (null != scenario && !scenario.isEmpty) {
      val s = ctx.get(RuntimeContext.KEY__SCENARIO)
      if (null == s) {
        ctx.put(RuntimeContext.KEY__SCENARIO, scenario)
        ctx.put(RuntimeContext.KEY__S, scenario)
      } else {
        s.asInstanceOf[util.Map[Any, Any]].putAll(scenario)
      }
    }
    this
  }

  def eraseScenario(): RuntimeContext = {
    ctx.remove(RuntimeContext.KEY__SCENARIO)
    ctx.remove(RuntimeContext.KEY__S)
    this
  }

  def setPrevCurrentData(prevContext: util.Map[Any, Any]): RuntimeContext = {
    if (null != prevContext && !prevContext.isEmpty) {
      ctx.put(RuntimeContext.KEY__P, prevContext)
      ctx.put(RuntimeContext.KEY__PREV, prevContext)
      val cases = ctx.get(RuntimeContext.KEY__CASES)
      if (null != cases) {
        cases.asInstanceOf[util.ArrayList[util.Map[Any, Any]]].add(prevContext)
      } else {
        val list = new util.ArrayList[util.Map[Any, Any]]()
        list.add(prevContext)
        ctx.put(RuntimeContext.KEY__CASES, list)
        ctx.put(RuntimeContext.KEY__C, list)
      }
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
