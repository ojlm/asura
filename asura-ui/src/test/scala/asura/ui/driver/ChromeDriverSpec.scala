package asura.ui.driver

import java.util

import asura.ui.karate.KarateRunner

object ChromeDriverSpec {

  def main(args: Array[String]): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("userDataDir", "logs/chrome")
    val driver = CustomChromeDriver.start(options, null)
    implicit val actions = KarateRunner.buildScenarioAction(driver.engine)
    KarateRunner.executeStep("driver 'https://github.com/'")
    KarateRunner.executeStep("delay(5000)")
    KarateRunner.executeStep("def targetUrl = 'https://github.com/search?q=asura+language:scala'")
    KarateRunner.executeStep("driver targetUrl")
  }

}
