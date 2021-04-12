package asura.core.script

import asura.common.util.LogUtils
import asura.core.script.builtin.{Functions, StringGenerator}
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import com.typesafe.scalalogging.Logger
import javax.script.{CompiledScript, ScriptContext, ScriptEngineManager, SimpleScriptContext}

/**
 * hold a javascript engine.
 * TODO: improve performance,
 * https://stackoverflow.com/questions/30140103/should-i-use-a-separate-scriptengine-and-compiledscript-instances-per-each-threa
 * https://stackoverflow.com/questions/42543730/how-can--rieuse-scriptcontext-or-otherwise-improve-performance?noredirect=1&lq=1
 */
object JavaScriptEngine {

  private val logger = Logger("JavaScriptEngine")
  // use separate execution context
  // private val engineExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
  val engine: GraalJSScriptEngine = {
    val sem = new ScriptEngineManager(getClass.getClassLoader)
    val jsEngine = sem.getEngineByName("javascript").asInstanceOf[GraalJSScriptEngine]
    jsEngine.put("polyglot.js.allowAllAccess", true)
    jsEngine
  }

  private val baseLibs: CompiledScript = {
    logger.info("initialize base javascript libraries")
    engine.compile(StringGenerator.exports + Functions.exports)
  }
  val localContext: ThreadLocal[ScriptContext] = ThreadLocal.withInitial(() => initScriptContext())

  def eval(script: String, bindingsData: java.util.Map[String, Any] = null): Any = {
    if (null != bindingsData) {
      val context = localContext.get()
      val bindings = context.getBindings(ScriptContext.ENGINE_SCOPE)
      bindings.putAll(bindingsData)
      val value = engine.eval(script, context)
      bindings.clear()
      value
    } else {
      engine.eval(script, localContext.get())
    }
  }

  def eval(script: CompiledScript, bindingsData: java.util.Map[String, Any]): Any = {
    val context = localContext.get()
    if (null != bindingsData) {
      val bindings = context.getBindings(ScriptContext.ENGINE_SCOPE)
      bindings.putAll(bindingsData)
      script.eval(context)
    } else {
      script.eval(context)
    }
  }

  // initialize a ScriptContext with base libraries
  private def initScriptContext(): ScriptContext = {
    val context = new SimpleScriptContext()
    try {
      context.setWriter(new CustomWriter())
      baseLibs.eval(context)
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
    }
    context
  }
}
