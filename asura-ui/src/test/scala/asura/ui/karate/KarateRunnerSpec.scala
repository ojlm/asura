package asura.ui.karate

object KarateRunnerSpec {

  def main(args: Array[String]): Unit = {
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
