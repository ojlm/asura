package asura.ui.cli

import java.util
import java.util.concurrent.ConcurrentHashMap

import asura.ui.driver.DriverProvider
import com.intuit.karate.core.ScenarioRuntime
import com.intuit.karate.driver.Driver

class DriverProviders extends DriverProvider {

  private val providers = new ConcurrentHashMap[String, DriverProvider]()
  private var default = DriverProviders.DEFAULT

  override def get(options: util.Map[String, AnyRef], sr: ScenarioRuntime): Driver = {
    val provider = providers.getOrDefault(options.getOrDefault("type", "chrome"), default)
    provider.get(options, sr)
  }

  override def release(driver: Driver): Unit = {
    val provider = providers.getOrDefault(driver.getOptions.`type`, default)
    provider.release(driver)
  }

  def register(`type`: String, provider: DriverProvider): Unit = {
    providers.put(`type`, provider)
  }

  def setDefault(default: DriverProvider): Unit = {
    this.default = default
  }

}

object DriverProviders {

  val INSTANCE = new DriverProviders()

  val DEFAULT = new DriverProvider {
    override def get(options: util.Map[String, AnyRef], sr: ScenarioRuntime): Driver = null

    override def release(driver: Driver): Unit = {}
  }

  def register(`type`: String, provider: DriverProvider): Unit = {
    INSTANCE.register(`type`, provider)
  }

  def setDefault(default: DriverProvider): Unit = {
    INSTANCE.setDefault(default)
  }

}
