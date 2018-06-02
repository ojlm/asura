package asura.web

import org.openqa.selenium.WebDriver

case class HandlerThread(r: Runnable, name: String) extends Thread(r: Runnable) {
  var driver: WebDriver = null
  setName(name)
  setDaemon(true)
}
