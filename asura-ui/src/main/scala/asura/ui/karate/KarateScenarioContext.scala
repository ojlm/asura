package asura.ui.karate

import com.intuit.karate.core.{FeatureContext, Scenario, ScenarioContext}
import com.intuit.karate.{CallContext, LogAppender, ScriptBindings}

case class KarateScenarioContext(
                                  _featureContext: FeatureContext,
                                  _call: CallContext,
                                  _classLoader: ClassLoader,
                                  _scenario: Scenario,
                                  _appender: LogAppender,
                                  extension: KarateExtension,
                                ) extends ScenarioContext(_featureContext, _call, _classLoader, _scenario, _appender) {

  KarateScenarioContext.doMore(this)

  override def copy(): ScenarioContext = {
    val copy = super.copy
    KarateScenarioContext.doMore(copy)
    copy
  }

  override def driver(expression: String): Unit = {
    super.driver(expression)
  }
}

object KarateScenarioContext {

  def doMore(ctx: ScenarioContext): Unit = {
    if (ctx.isInstanceOf[KarateScenarioContext]) {
      val extension = ctx.asInstanceOf[KarateScenarioContext].extension
      if (extension != null && extension.driver != null) {
        ctx.setDriver(extension.driver)
      }
    }
    ctx.bindings.adds.put(ScriptBindings.READ, readX(ctx))
  }

  // TODO: support read from other storage
  def readX(ctx: ScenarioContext): String => Object = (s: String) => {
    ctx.read.apply(s)
  }
}
