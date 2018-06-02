package asura.web

import java.net.URL

import asura.common.ScalaTestBaseSpec
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver

class RemoteDriverSpec extends ScalaTestBaseSpec {

  test("remote chrome driver") {
    val options = new ChromeOptions()
    options.addArguments("--headless", "--disable-gpu")
    val driver = new RemoteWebDriver(new URL("http://127.0.0.1:9515"), options)
    driver.get("https://baidu.com/")
    println(driver.getPageSource)
    driver.quit()
  }
}
