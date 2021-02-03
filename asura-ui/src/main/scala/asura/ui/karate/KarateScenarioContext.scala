package asura.ui.karate

import akka.pattern.ask
import asura.ui.UiConfig
import asura.ui.actor.ChromeDriverHolderActor.GetDriver
import asura.ui.driver.Drivers
import com.intuit.karate.core.{FeatureContext, Scenario, ScenarioContext}
import com.intuit.karate.driver.Driver
import com.intuit.karate.{CallContext, LogAppender, ScriptBindings}

class KarateScenarioContext(
                             _featureContext: FeatureContext,
                             _call: CallContext,
                             _classLoader: ClassLoader,
                             _scenario: Scenario,
                             _appender: LogAppender
                           ) extends ScenarioContext(_featureContext, _call, _classLoader, _scenario, _appender) {

  KarateScenarioContext.doMore(this)

  def this(featureContext: FeatureContext, call: CallContext, scenario: Scenario, appender: LogAppender) = {
    this(featureContext, call, null, scenario, appender)
  }

  override def copy: ScenarioContext = {
    val copy = super.copy
    KarateScenarioContext.doMore(copy)
    copy
  }

  override def driver(expression: String): Unit = {
    val config = getConfig
    val driverOptions = config.getDriverOptions
    if (UiConfig.driverHolder != null && driverOptions != null) {
      val `type` = driverOptions.get("type")
      if (`type` == null || `type` == "chrome") {
        import asura.common.util.FutureUtils.RichFuture
        import asura.ui.UiConfig.DEFAULT_ACTOR_ASK_TIMEOUT
        val driver = (UiConfig.driverHolder ? GetDriver(Drivers.CHROME)).await.asInstanceOf[Driver]
        setDriver(driver)
      }
    }
    super.driver(expression)
  }
}

object KarateScenarioContext {

  def doMore(ctx: ScenarioContext): Unit = {
    ctx.bindings.adds.put(ScriptBindings.READ, readX(ctx))
  }

  // TODO: support read from other storage
  def readX(ctx: ScenarioContext): String => Object = (s: String) => {
    ctx.read.apply(s)
  }
}
