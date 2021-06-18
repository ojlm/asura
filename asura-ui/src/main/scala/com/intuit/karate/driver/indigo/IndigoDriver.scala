package com.intuit.karate.driver.indigo

import java.util
import java.util.Base64
import java.util.stream.Collectors

import asura.common.util.JsonUtils
import asura.ui.message.IndigoMessage
import asura.ui.message.builder.SendKeysToElement.SendKeysModel
import asura.ui.message.builder._
import com.intuit.karate.core.{AutoDef, Plugin, ScenarioRuntime}
import com.intuit.karate.driver.indigo.IndigoDriver.logger
import com.intuit.karate.driver.{Driver, DriverElement, DriverOptions, Element}
import com.intuit.karate.http.{ResourceType, WebSocketClient, WebSocketOptions}
import com.intuit.karate.{Logger, StringUtils}

class IndigoDriver(options: DriverOptions, url: String) extends Driver {

  private var id = 0
  private var terminated = false
  private val reqWait = IndigoWait(options, this)
  private val wsClient: WebSocketClient = {
    val wsOptions = new WebSocketOptions(url)
    wsOptions.setMaxPayloadSize(options.maxPayloadSize)
    wsOptions.setTextConsumer(text => {
      if (logger.isTraceEnabled) {
        logger.trace(s"<< $text")
      } else {
        logger.debug(s"<< ${StringUtils.truncate(text, 1024, true)}")
      }
      receive(JsonUtils.parse(text, classOf[IndigoMessage]))
    })
    new WebSocketClient(wsOptions, logger)
  }
  private implicit val SendFunction: IndigoMessage => IndigoMessage = sendAndWait
  val sessionId: String = {
    val map = options.getWebDriverSessionPayload()
    NewSession(map).withId(nextId()).send().resBodyAs(classOf[NewSession.Response]).sessionId
  }

  override def activate(): Unit = doNotSupport()

  override def refresh(): Unit = doNotSupport()

  override def reload(): Unit = doNotSupport()

  override def back(): Unit = {
    PressBack(sessionId).withId(nextId()).send().ok()
  }

  override def forward(): Unit = doNotSupport()

  override def maximize(): Unit = doNotSupport()

  override def minimize(): Unit = doNotSupport()

  override def fullscreen(): Unit = doNotSupport()

  override def close(): Unit = {
    terminated = true
  }

  override def quit(): Unit = {
    terminated = true
  }

  override def switchPage(titleOrUrl: String): Unit = doNotSupport()

  override def switchPage(index: Int): Unit = doNotSupport()

  override def switchFrame(index: Int): Unit = doNotSupport()

  override def switchFrame(locator: String): Unit = doNotSupport()

  override def getUrl(): String = doNotSupport()

  override def setUrl(url: String): Unit = {}

  override def getDimensions(): util.Map[String, AnyRef] = {
    val map = GetDeviceSize(sessionId).withId(nextId())
      .send().resBodyAsMap.get("value").asInstanceOf[util.Map[String, AnyRef]]
    map.put("x", Int.box(0))
    map.put("y", Int.box(0))
    map
  }

  override def setDimensions(map: util.Map[String, AnyRef]): Unit = doNotSupport()

  override def getTitle(): String = doNotSupport()

  override def getPages(): util.List[String] = doNotSupport()

  override def getDialogText(): String = doNotSupport()

  override def screenshot(embed: Boolean): Array[Byte] = {
    screenshot(null, embed)
  }

  override def cookie(name: String): util.Map[String, AnyRef] = doNotSupport()

  override def cookie(cookie: util.Map[String, AnyRef]): Unit = doNotSupport()

  override def deleteCookie(name: String): Unit = doNotSupport()

  override def clearCookies(): Unit = doNotSupport()

  override def getCookies(): util.List[util.Map[_, _]] = doNotSupport()

  override def dialog(accept: Boolean): Unit = doNotSupport()

  override def dialog(accept: Boolean, input: String): Unit = doNotSupport()

  override def script(expression: String): AnyRef = doNotSupport()

  override def waitUntil(expression: String): Boolean = doNotSupport()

  override def submit(): Driver = doNotSupport()

  override def timeout(millis: Integer): Driver = {
    options.setTimeout(millis)
    this
  }

  override def timeout(): Driver = {
    timeout(null)
  }

  override def focus(locator: String): Element = doNotSupport()

  override def clear(locator: String): Element = {
    Clear(sessionId, elementId(locator)).withId(nextId())
      .send().ok()
    DriverElement.locatorExists(this, locator)
  }

  override def click(locator: String): Element = {
    Click(sessionId, elementId(locator)).withId(nextId())
      .send().ok()
    DriverElement.locatorExists(this, locator)
  }

  override def input(locator: String, value: String): Element = {
    SendKeysToElement(sessionId, elementId(locator), SendKeysModel(value)).withId(nextId())
      .send().ok()
    DriverElement.locatorExists(this, locator)
  }

  override def select(locator: String, text: String): Element = doNotSupport()

  override def select(locator: String, index: Int): Element = doNotSupport()

  override def value(locator: String, value: String): Element = doNotSupport()

  override def actions(actions: util.List[util.Map[String, AnyRef]]): Unit = {
    W3CActions(sessionId, actions).withId(nextId())
      .send().ok()
  }

  override def html(locator: String): String = doNotSupport()

  override def text(locator: String): String = {
    GetText(sessionId, elementId(locator)).withId(nextId())
      .send().resBodyAs(classOf[GetText.Response]).value
  }

  override def value(locator: String): String = doNotSupport()

  override def attribute(locator: String, name: String): String = {
    GetElementAttribute(sessionId, elementId(locator), name).withId(nextId())
      .send().resBodyAs(classOf[GetElementAttribute.Response]).value
  }

  override def property(locator: String, name: String): String = doNotSupport()

  override def enabled(locator: String): Boolean = doNotSupport()

  override def position(locator: String): util.Map[String, AnyRef] = {
    GetRect(sessionId, elementId(locator)).withId(nextId())
      .send().resBodyAsMap.get("value").asInstanceOf[util.Map[String, AnyRef]]
  }

  override def screenshot(locator: String, embed: Boolean): Array[Byte] = {
    val message = if (locator == null) {
      CaptureScreenshot(sessionId).withId(nextId())
    } else {
      GetElementScreenshot(sessionId, elementId(locator)).withId(nextId())
    }
    val src = message.send().resBodyAs(classOf[StringAppiumResponse]).value
    val bytes = Base64.getMimeDecoder().decode(src)
    if (embed) {
      getRuntime().embed(bytes, ResourceType.PNG)
    }
    bytes
  }

  override def pdf(options: util.Map[String, AnyRef]): Array[Byte] = doNotSupport()

  override def isTerminated: Boolean = {
    terminated
  }

  override def getOptions(): DriverOptions = {
    options
  }

  override def elementId(locator: String): String = {
    FindElement(sessionId, locator).withId(nextId())
      .send()
      .resBodyAs(classOf[ElementModelAppiumResponse]).value.jwpElementId
  }

  override def elementIds(locator: String): util.List[_] = {
    FindElements(sessionId, locator).withId(nextId())
      .send()
      .resBodyAs(classOf[ElementModelsAppiumResponse]).value
      .stream().map(item => item.jwpElementId).collect(Collectors.toList[String]())
  }

  // custom functions
  override def methodNames(): util.List[String] = IndigoDriver.METHOD_NAMES

  @AutoDef
  def status(): util.Map[_, _] = {
    Status().withId(nextId()).send().resBodyAsMap
  }

  @AutoDef
  def source(): String = {
    Source(sessionId).withId(nextId()).send().resBodyAs(classOf[Source.Response]).value
  }

  def send(req: IndigoMessage): Unit = {
    val json = JsonUtils.stringify(req)
    logger.debug(s">> $json")
    wsClient.send(json)
  }

  def sendAndWait(req: IndigoMessage): IndigoMessage = {
    val res = reqWait.send(req)
    if (res == null) {
      throw new RuntimeException(s"failed to get reply for id: ${req.id}, uri: ${req.req.uri}")
    }
    res
  }

  def receive(res: IndigoMessage): Unit = {
    reqWait.receive(res)
  }

  def nextId(): Int = {
    id = id + 1
    id
  }

  @throws[RuntimeException]
  def doNotSupport[T](): T = {
    throw new RuntimeException("Current driver is not support this function.")
  }

}

object IndigoDriver {

  val logger = new Logger("IndigoDriver")
  val METHOD_NAMES = {
    val names = new util.HashSet[String]()
    names.addAll(Plugin.methodNames(classOf[Driver]))
    names.addAll(Plugin.methodNames(classOf[IndigoDriver]))
    new util.ArrayList[String](names)
  }

  def start(map: util.Map[String, AnyRef], sr: ScenarioRuntime): IndigoDriver = {
    val serial = map.get("serial").asInstanceOf[String]
    if (StringUtils.isBlank(serial)) throw new RuntimeException("Serial is not set")
    val options = new DriverOptions(map, sr, 8080, "java")
    options.arg("-jar")
    options.arg("indigo.jar")
    options.arg("android")
    if (options.start) { // todo: start a process
    }
    var url: String = null
    if (map.containsKey("webSocketDebuggerUrl")) {
      url = map.get("webSocketDebuggerUrl").asInstanceOf[String]
    } else if (map.containsKey("debuggerUrl")) {
      url = map.get("debuggerUrl").asInstanceOf[String]
    } else {
      url = s"ws://${options.host}:${options.port}/api/ws/device/$serial/wd"
    }
    new IndigoDriver(options, url)
  }

}
