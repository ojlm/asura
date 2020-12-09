package asura.ui.driver

class ChromeDriverSpec {

}

object ChromeDriverSpec {

  def main(args: Array[String]): Unit = {
    val driver = CustomChromeDriver.start(true, null)
    driver.setUrl("https://github.com/")
  }

}
