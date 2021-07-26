package asura.ui

import java.util

import com.intuit.karate.driver.chrome.Chrome

trait BaseSpec {

  def openDriver(): Chrome = {
    val options = new util.HashMap[String, Object]()
    options.put("userDataDir", "logs/chrome")
    options.put("port", Int.box(9222))
    options.put("start", Boolean.box(false))
    val chrome = Chrome.start(options, null, true)
    chrome
  }

}
