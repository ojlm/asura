package asura.ui.driver

import com.intuit.karate.driver.Driver

trait DriverProvider {

  def get(`type`: String, options: java.util.Map[String, AnyRef]): Driver

  def release(driver: Driver): Unit

}
