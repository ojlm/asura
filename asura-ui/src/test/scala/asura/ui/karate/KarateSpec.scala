package asura.ui.karate

import com.intuit.karate.ScenarioActions
import com.intuit.karate.core.{ScenarioEngine, StepRuntime}

object KarateSpec {

  def main(args: Array[String]): Unit = {
    val engine = ScenarioEngine.forTempUse()
    val actions = new ScenarioActions(engine)
    StepRuntime.execute(null, actions)
    val result = KarateRunner.runFeature(text)
    println(result)
  }

  val text =
    """Feature: Feature Demo
      |
      |  Scenario: Scenario Demo
      |    * configure driver = { type: 'chrome', start: true, 'userDataDir': 'logs/chrome'}
      |    * driver 'https://github.com/'
      |    * delay(2000)
      |    * def targetUrl = 'https://github.com/search?q=asura+language:scala'
      |    * driver targetUrl
      |    * screenshot()
      |    * delay(2000)
      |""".stripMargin
}
