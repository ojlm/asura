package asura.ui.driver

import java.util

import asura.ui.karate.KarateRunner
import com.intuit.karate.driver.chrome.Chrome

object ChromeDriverSpec {

  def main(args: Array[String]): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("userDataDir", "logs/chrome")
    options.put("port", Int.box(9222));
    val driver = Chrome.start(options, null, true)
    implicit val actions = KarateRunner.buildScenarioAction(driver.engine)
    KarateRunner.executeStep("driver 'https://github.com/'")
    KarateRunner.executeStep("delay(2000)")
    KarateRunner.executeStep("def targetUrl = 'https://github.com/search?q=asura+language:scala'")
    KarateRunner.executeStep("driver targetUrl")
    KarateRunner.executeStep("等待 2 秒")
    KarateRunner.executeStep("打开 'https://github.com/' 网页")
    KarateRunner.executeStep("点击 '{}Sign in' 元素")
    KarateRunner.executeStep("在 '#login_field' 元素输入 'abc'")
  }

}
