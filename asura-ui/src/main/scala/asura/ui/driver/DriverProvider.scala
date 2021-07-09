package asura.ui.driver

import com.intuit.karate.core.ScenarioRuntime
import com.intuit.karate.driver.Driver

trait DriverProvider {

  def get(options: java.util.Map[String, AnyRef], sr: ScenarioRuntime): Driver

  def release(driver: Driver): Unit

}
