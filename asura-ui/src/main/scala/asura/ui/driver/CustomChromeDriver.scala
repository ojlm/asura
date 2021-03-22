package asura.ui.driver

import java.util

import asura.common.util.StringUtils
import asura.ui.karate.KarateRunner
import com.intuit.karate.core.ScenarioEngine
import com.intuit.karate.driver.chrome.Chrome
import com.intuit.karate.driver.{DevToolsMessage, DriverOptions, Input, Keys}
import com.intuit.karate.shell.Command
import com.intuit.karate.{FileUtils, Json}

/**
 * the operation of this driver is blocked
 */
class CustomChromeDriver(
                          options: DriverOptions,
                          val engine: ScenarioEngine,
                          command: Command,
                          webSocketUrl: String,
                          val filter: util.Map[String, AnyRef] => Unit,
                          inject: Boolean,
                        ) extends Chrome(options, command, webSocketUrl) {

  // filter websocket message
  client.setTextHandler(text => {
    val map: util.Map[String, AnyRef] = Json.of(text).value
    val msg = new DevToolsMessage(this, map)
    if (filter != null && StringUtils.isNotEmpty(msg.getMethod())) {
      filter(map)
    }
    receive(msg)
    false // no async signalling, for normal use, e.g. chrome developer tools
  })
  this.activate()
  this.enableLog()
  this.setDiscoverTargets()
  this.enablePageEvents()
  this.enableRuntimeEvents()
  this.enableTargetEvents()
  if (!options.headless) {
    this.initWindowIdAndState()
  }
  if (inject) {
    engine.setDriver(this)
  }

  def closeClient(): Unit = {
    client.close()
  }

  def realQuit(): Unit = {
    super.quit()
  }

  // do not quit
  override def quit(): Unit = {}

  // do not close
  override def close(): Unit = {}

  // https://chromedevtools.github.io/devtools-protocol/tot/Log/#method-enable
  def enableLog(): Unit = {
    method("Log.enable").send()
  }

  def enableDom(): Unit = {
    method("DOM.enable").send()
  }

  def setDiscoverTargets(): Unit = {
    method("Target.setDiscoverTargets").param("discover", true).send()
  }

  // do not need to locate an element
  def input(value: String): Unit = {
    val input = new Input(value)
    while (input.hasNext) {
      val c = input.next
      val modifiers = input.getModifierFlags
      val keyCode = Keys.code(c)
      if (keyCode != null) {
        sendKey(c, modifiers, "rawKeyDown", keyCode)
        sendKey(c, modifiers, "char", keyCode)
        sendKey(c, modifiers, "keyUp", keyCode)
      } else {
        sendKey(c, modifiers, "char", -1)
      }
    }
  }

  def sendKey(c: Char, modifiers: Int, `type`: String, keyCode: Int): Unit = {
    if (keyCode == 9 && "char" == `type`) {
      // special case
    } else {
      val dtm = method("Input.dispatchKeyEvent")
        .param("modifiers", modifiers)
        .param("type", `type`)
      if (keyCode == -1) {
        dtm.param("text", s"$c")
      } else {
        keyCode match {
          case 13 =>
            dtm.param("text", "\r") // important ! \n does NOT work for chrome
          case 9 => // TAB
            dtm.param("text", "")
          case 46 => // DOT
            if ("rawKeyDown" == `type`) dtm.param("type", "keyDown")
            dtm.param("text", ".")
          case _ =>
            dtm.param("text", s"$c")
        }
        dtm.param("windowsVirtualKeyCode", keyCode)
      }
      dtm.send()
    }
  }

  def screenshotAsBase64(): String = {
    screenshot(false)
    val dtm = method("Page.captureScreenshot").send()
    dtm.getResult("data").getAsString()
  }

}

object CustomChromeDriver {

  val CODES: util.Map[Character, Integer] = {
    val field = classOf[Keys].getDeclaredField("CODES")
    field.setAccessible(true)
    field.get(null).asInstanceOf[util.Map[Character, Integer]]
  }

  def start(
             newChrome: Boolean,
             filter: util.Map[String, AnyRef] => Unit,
             inject: Boolean,
           ): CustomChromeDriver = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(newChrome))
    start(options, KarateRunner.buildScenarioEngine(), filter, inject)
  }

  def start(
             options: util.HashMap[String, Object],
             filter: util.Map[String, AnyRef] => Unit,
             inject: Boolean,
           ): CustomChromeDriver = {
    start(options, KarateRunner.buildScenarioEngine(), filter, inject)
  }

  def start(
             map: util.Map[String, Object],
             engine: ScenarioEngine,
             filter: util.Map[String, AnyRef] => Unit,
             inject: Boolean,
           ): CustomChromeDriver = {
    val defaultExecutable = if (FileUtils.isOsWindows) {
      Chrome.DEFAULT_PATH_WIN
    } else if (FileUtils.isOsMacOsX) {
      Chrome.DEFAULT_PATH_MAC
    } else {
      Chrome.DEFAULT_PATH_LINUX
    }
    val options = new DriverOptions(map, engine.runtime, 9222, defaultExecutable)
    options.arg("--remote-debugging-port=" + options.port)
    options.arg("--no-first-run")
    if (options.userDataDir != null) {
      options.arg("--user-data-dir=" + options.userDataDir)
    }
    options.arg("--disable-popup-blocking")
    if (options.headless) {
      options.arg("--headless")
    }
    val command = options.startProcess
    var webSocketUrl: String = null
    var attachUrl: String = null
    val startUrl = map.get("startUrl")
    if (map.containsKey("debuggerUrl")) {
      webSocketUrl = map.get("debuggerUrl").asInstanceOf[String]
    } else {
      val http = options.getHttp
      Command.waitForHttp(http.urlBase + "/json")
      val res = http.path("json").get
      if (res.json().asList.isEmpty) {
        if (command != null) command.close(true)
        throw new RuntimeException("chrome server returned empty list from " + http.urlBase)
      }
      import scala.jdk.CollectionConverters.CollectionHasAsScala
      val targets = res.json().asList.asInstanceOf[util.List[util.Map[String, Object]]].asScala
      var found = false
      for (target <- targets if !found) {
        val targetUrl = target.get("url").asInstanceOf[String]
        if (targetUrl == null || targetUrl.startsWith("chrome-")) {
          // ignore
        } else {
          if (startUrl != null) {
            if (targetUrl.equals(startUrl)) {
              webSocketUrl = target.get("webSocketDebuggerUrl").asInstanceOf[String]
              found = true
            }
          } else {
            val targetType = target.get("type").asInstanceOf[String]
            if ("page" == targetType) {
              webSocketUrl = target.get("webSocketDebuggerUrl").asInstanceOf[String]
              if (options.attach == null) { // take the first
                found = true
              } else if (targetUrl.contains(options.attach)) {
                attachUrl = targetUrl
                found = true
              }
            }
          }
        }
      }
    }
    if (webSocketUrl == null) {
      throw new RuntimeException("failed to attach to chrome debug server")
    }
    new CustomChromeDriver(options, engine, command, webSocketUrl, filter, inject)
  }

}
