package asura.core.cs

import java.util

import asura.common.util.StringUtils
import asura.core.script.JavaScriptEngine
import asura.core.util.{JsonPathUtils, StringTemplate}

object CaseContext {

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

  def apply(): CaseContext = new CaseContext()

  def apply(rawContext: util.Map[Any, Any]) = new CaseContext(rawContext)

  def extractCaseSelfContext(caseResult: CaseResult): util.Map[Any, Any] = {
    val context = caseResult.context
    val selfContext = new util.HashMap[Any, Any]()
    if (null != context && !context.isEmpty) {
      val status = context.get(CaseContext.KEY_STATUS)
      if (Option(status).isDefined) selfContext.put(CaseContext.KEY_STATUS, status)
      val headers = context.get(CaseContext.KEY_HEADERS)
      if (null != headers) selfContext.put(CaseContext.KEY_HEADERS, headers)
      val entity = context.get(CaseContext.KEY_ENTITY)
      if (null != entity) selfContext.put(CaseContext.KEY_ENTITY, entity)
    }
    selfContext
  }
}

/**
  * use java type system
  */
case class CaseContext(
                        private val ctx: util.Map[Any, Any] = new util.HashMap[Any, Any](),
                        val options: ContextOptions = null,
                      ) {

  def rawContext = ctx

  def setOrUpdateGlobal(global: util.Map[Any, Any]): CaseContext = {
    if (null != global && !global.isEmpty) {
      val g = ctx.get(CaseContext.KEY__GLOBAL)
      if (null == g) {
        ctx.put(CaseContext.KEY__GLOBAL, global)
        ctx.put(CaseContext.KEY__G, global)
      } else {
        g.asInstanceOf[util.Map[Any, Any]].putAll(global)
      }
    }
    this
  }

  def eraseGlobal(): CaseContext = {
    ctx.remove(CaseContext.KEY__GLOBAL)
    ctx.remove(CaseContext.KEY__G)
    this
  }

  def setOrUpdateJob(job: util.Map[Any, Any]): CaseContext = {
    if (null != job && !job.isEmpty) {
      val j = ctx.get(CaseContext.KEY__JOB)
      if (null == j) {
        ctx.put(CaseContext.KEY__JOB, job)
        ctx.put(CaseContext.KEY__J, job)
      } else {
        j.asInstanceOf[util.Map[Any, Any]].putAll(job)
      }
    }
    this
  }

  def eraseJob(): CaseContext = {
    ctx.remove(CaseContext.KEY__JOB)
    ctx.remove(CaseContext.KEY__J)
    this
  }

  def setOrUpdateScenario(scenario: util.Map[Any, Any]): CaseContext = {
    if (null != scenario && !scenario.isEmpty) {
      val s = ctx.get(CaseContext.KEY__SCENARIO)
      if (null == s) {
        ctx.put(CaseContext.KEY__SCENARIO, scenario)
        ctx.put(CaseContext.KEY__S, scenario)
      } else {
        s.asInstanceOf[util.Map[Any, Any]].putAll(scenario)
      }
    }
    this
  }

  def eraseScenario(): CaseContext = {
    ctx.remove(CaseContext.KEY__SCENARIO)
    ctx.remove(CaseContext.KEY__S)
    this
  }

  def setPrevCurrentData(prevContext: util.Map[Any, Any]): CaseContext = {
    if (null != prevContext && !prevContext.isEmpty) {
      ctx.put(CaseContext.KEY__P, prevContext)
      ctx.put(CaseContext.KEY__PREV, prevContext)
      val cases = ctx.get(CaseContext.KEY__CASES)
      if (null != cases) {
        cases.asInstanceOf[util.ArrayList[util.Map[Any, Any]]].add(prevContext)
      } else {
        val list = new util.ArrayList[util.Map[Any, Any]]()
        list.add(prevContext)
        ctx.put(CaseContext.KEY__CASES, list)
        ctx.put(CaseContext.KEY__C, list)
      }
    }
    this
  }

  def eraseCurrentData(): CaseContext = {
    ctx.remove(CaseContext.KEY_STATUS)
    ctx.remove(CaseContext.KEY_HEADERS)
    ctx.remove(CaseContext.KEY_ENTITY)
    this
  }

  def setCurrentStatus(status: Any): CaseContext = {
    ctx.put(CaseContext.KEY_STATUS, status)
    this
  }

  def setCurrentHeaders(headers: Any): CaseContext = {
    ctx.put(CaseContext.KEY_HEADERS, headers)
    this
  }

  def setCurrentEntity(entity: Any): CaseContext = {
    ctx.put(CaseContext.KEY_ENTITY, entity)
    this
  }

  def renderSingleMacroAsString(tpl: String): String = {
    CaseContext.render(tpl, ctx).toString
  }

  def renderBodyAsString(tpl: String): String = {
    CaseContext.renderBody(tpl, ctx).toString
  }
}
