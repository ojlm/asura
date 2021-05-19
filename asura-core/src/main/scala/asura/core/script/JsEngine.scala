package asura.core.script

import asura.core.script.builtin.{Functions, StringGenerator}
import com.typesafe.scalalogging.Logger
import org.graalvm.polyglot.{Context, Engine, Source, Value}

case class JsEngine(context: Context) {

  val bindings: Value = context.getBindings(JsEngine.JS)
  context.eval(JsEngine.BASE_LIBS)

  def eval(exp: String, bindingsData: java.util.Map[String, Any] = null): Object = {
    if (bindingsData != null) {
      putAll(bindingsData)
    }
    val value = context.eval(JsEngine.JS, exp)
    if (value.isNull) null else value.as(classOf[Object])
  }

  def put(key: String, value: Any): Unit = {
    bindings.putMember(key, value)
  }

  def putAll(map: java.util.Map[String, Any]): Unit = {
    map.forEach((k: String, v: Any) => put(k, v))
  }

  def close(cancelIfExecuting: Boolean = true): Unit = {
    context.close(cancelIfExecuting)
  }

}

object JsEngine {

  private val logger = Logger("JsEngine")

  val JS = "js"
  val JS_EXPERIMENTAL_FOP = "js.experimental-foreign-object-prototype"
  val JS_NASHORN_COMPAT = "js.nashorn-compat"
  val TRUE = "true"

  val BASE_LIBS: Source = {
    logger.info("initialize base javascript libraries")
    Source.newBuilder(JS, StringGenerator.exports + Functions.exports, "base").build()
  }

  def createContext(engine: Engine): Context = {
    Context.newBuilder(JS)
      .allowExperimentalOptions(true)
      .allowAllAccess(true)
      .option(JS_NASHORN_COMPAT, TRUE)
      .option(JS_EXPERIMENTAL_FOP, TRUE)
      .engine(if (engine != null) engine else Engine.newBuilder().build())
      .build()
  }

  private val GLOBAL_JS_ENGINE: ThreadLocal[JsEngine] = ThreadLocal.withInitial(() => {
    JsEngine(createContext(null))
  })

  def global(): JsEngine = GLOBAL_JS_ENGINE.get()

  def local(): JsEngine = JsEngine(createContext(GLOBAL_JS_ENGINE.get().context.getEngine))

  def eval(script: String, bindingsData: java.util.Map[String, Any] = null): Any = {
    val engine = local()
    val value = engine.eval(script, bindingsData)
    engine.close()
    value
  }

  def evalGlobal(script: String, bindingsData: java.util.Map[String, Any] = null): Any = {
    global().eval(script, bindingsData)
  }

}
