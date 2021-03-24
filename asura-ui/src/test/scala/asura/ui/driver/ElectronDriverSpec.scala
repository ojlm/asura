package asura.ui.driver

import java.util
import java.util.function.Consumer

import com.intuit.karate.driver.chrome.Chrome

object ElectronDriverSpec {

  def main(args: Array[String]): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(false))
    options.put("port", Int.box(9221))
    // options.put("startUrl", "app.html")
    options.put("debuggerUrl", "ws://localhost:9221/devtools/page/B581D9CFFDDF7E903D14F86C48B88D89")
    val filter: Consumer[util.Map[String, AnyRef]] = params => {
      println(s"======> ${params}")
    }
    Chrome.start(options, filter, true)
  }

}
