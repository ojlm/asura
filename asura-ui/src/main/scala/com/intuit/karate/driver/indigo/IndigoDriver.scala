package com.intuit.karate.driver.indigo

import java.nio.charset.StandardCharsets
import java.util
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

import scala.concurrent.duration._

import asura.common.util.{FutureUtils, HttpUtils, JsonUtils}
import asura.ui.hub.Hubs.{DeviceWdHub, IndigoAppiumHub}
import asura.ui.hub.Sink
import asura.ui.message.IndigoMessage
import asura.ui.message.builder.{GetClipboard, _}
import asura.ui.model.Position
import com.intuit.karate.core.{AutoDef, Plugin, ScenarioRuntime}
import com.intuit.karate.driver._
import com.intuit.karate.driver.indigo.IndigoDriver.logger
import com.intuit.karate.http.{ResourceType, WebSocketClient, WebSocketOptions}
import com.intuit.karate.{Logger, StringUtils}

class IndigoDriver(options: DriverOptions, url: String, useHub: Boolean) extends Driver {

  private val serial = options.options.get("serial").asInstanceOf[String]
  private var id = 0
  private var terminated = false
  private val reqWait = IndigoWait(options, this)

  private var deviceAppiumSinks: ConcurrentHashMap[Sink[IndigoMessage], Sink[IndigoMessage]] = null
  private var deviceWdSink: Sink[IndigoMessage] = null
  private var wsClient: WebSocketClient = null
  if (useHub) {
    deviceAppiumSinks = IndigoAppiumHub.getSinks(serial)
    deviceWdSink = new Sink[IndigoMessage]() {
      override def write(frame: IndigoMessage): Boolean = {
        receive(frame)
        true
      }
    }
    DeviceWdHub.enter(serial, deviceWdSink)
  } else {
    wsClient = {
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
  }

  implicit val SendFunction: IndigoMessage => IndigoMessage = sendAndWait
  val sessionId: String = {
    if (useHub) {
      SESSION_NO_ID
    } else {
      val map = options.getWebDriverSessionPayload()
      NewSession(map).withId(nextId()).send().resBodyAs(classOf[NewSession.Response]).sessionId
    }
  }
  var dimensions: util.Map[String, AnyRef] = null
  val gestures = new IndigoGestures(this)
  getRuntime.engine.setHiddenVariable("gestures", gestures)

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
    if (wsClient != null) wsClient.close()
    if (deviceWdSink != null) DeviceWdHub.leave(serial, deviceWdSink)
  }

  override def quit(): Unit = {
    close()
  }

  override def switchPage(titleOrUrl: String): Unit = doNotSupport()

  override def switchPage(index: Int): Unit = doNotSupport()

  override def switchFrame(index: Int): Unit = doNotSupport()

  override def switchFrame(locator: String): Unit = doNotSupport()

  override def getUrl(): String = doNotSupport()

  override def setUrl(url: String): Unit = {}

  override def getDimensions(): util.Map[String, AnyRef] = {
    if (dimensions == null) {
      dimensions = GetDeviceSize(sessionId).withId(nextId()).send().resBodyValueAsMap
      dimensions.put("x", Int.box(0))
      dimensions.put("y", Int.box(0))
    }
    dimensions
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
    SendKeysToElement(sessionId, elementId(locator), SendKeysToElement.SendKeysModel(value))
      .withId(nextId()).send().ok()
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

  def rect(locator: String): ElementRectModel = {
    GetRect(sessionId, elementId(locator)).withId(nextId())
      .send().resBodyAs(classOf[GetRect.Response]).value
  }

  override def position(locator: String): util.Map[String, AnyRef] = {
    GetRect(sessionId, elementId(locator)).withId(nextId()).send().resBodyValueAsMap
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

  override def mouse(): Mouse = IndigoMouse(this)

  override def mouse(locator: String): Mouse = IndigoMouse(this, locator)

  override def mouse(x: Int, y: Int): Mouse = IndigoMouse(this, x, y)

  // custom functions
  override def methodNames(): util.List[String] = IndigoDriver.METHOD_NAMES

  @AutoDef
  def status(): util.Map[_, _] = {
    Status().withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def sessions(): util.List[util.Map[_, _]] = {
    GetSessions().withId(nextId()).send().resBodyValue.asInstanceOf[util.List[util.Map[_, _]]]
  }

  @AutoDef
  def session(): util.Map[_, _] = {
    GetSessionDetails(sessionId).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def orientation(): String = {
    GetOrientation(sessionId).withId(nextId()).send().resBodyAs(classOf[GetOrientation.Response]).value
  }

  @AutoDef
  def rotation(): util.Map[_, _] = {
    GetRotation(sessionId).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def size(locator: String): util.Map[_, _] = {
    GetSize(sessionId, elementId(locator)).send().resBodyValueAsMap
  }

  @AutoDef
  def name(locator: String): String = {
    GetName(sessionId, elementId(locator)).send().resBodyAs(classOf[GetName.Response]).value
  }

  @AutoDef
  def location(locator: String): util.Map[_, _] = {
    Location(sessionId, elementId(locator)).send().resBodyValueAsMap
  }

  @AutoDef
  def source(): String = {
    Source(sessionId).withId(nextId()).send().resBodyAs(classOf[Source.Response]).value
  }

  @AutoDef
  def deviceInfo(): util.Map[_, _] = {
    GetDeviceInfo(sessionId).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def deviceSize(): util.Map[_, _] = {
    GetDeviceSize(sessionId).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def devicePixelRatio(): Float = {
    GetDevicePixelRatio(sessionId).withId(nextId()).send().resBodyAs(classOf[GetDevicePixelRatio.Response]).value
  }

  @AutoDef
  def systemBars(): util.Map[_, _] = {
    GetSystemBars(sessionId).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def battery(): util.Map[_, _] = {
    GetBatteryInfo(sessionId).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def firstVisibleView(locator: String): util.Map[_, _] = {
    FirstVisibleView(sessionId, elementId(locator)).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def settings(): util.Map[_, _] = {
    GetSettings(sessionId).withId(nextId()).send().resBodyValueAsMap
  }

  @AutoDef
  def alertText(): String = {
    GetAlertText(sessionId).withId(nextId()).send().resBodyAs(classOf[GetAlertText.Response]).value
  }

  def parseTapPoint(x: Any, y: Any): (Double, Double) = {
    val tupleX = Position.getParseResult(x)
    val tupleY = Position.getParseResult(y)
    val dimensions = getDimensions()
    val posX = if (tupleX._1 != -1) {
      tupleX._1
    } else {
      dimensions.get("width").asInstanceOf[Int] * tupleX._2
    }
    val posY = if (tupleY._1 != -1) {
      tupleY._1
    } else {
      dimensions.get("height").asInstanceOf[Int] * tupleY._2
    }
    (posX, posY)
  }

  @AutoDef
  def click(x: Any, y: Any): Unit = {
    tap(x, y)
  }

  @AutoDef
  def tap(x: Any, y: Any): Unit = {
    val pos = parseTapPoint(x, y)
    Tap(sessionId, pos._1, pos._2).withId(nextId()).send().ok()
  }

  def tap(body: Tap.TapModel): Unit = {
    Tap(sessionId, body).withId(nextId()).send().ok()
  }

  @AutoDef
  def orientation(orientation: String): String = {
    SetOrientation(sessionId, orientation).withId(nextId())
      .send().resBodyAs(classOf[SetOrientation.Response]).value
  }

  @AutoDef
  def rotation(z: Int): String = {
    SetRotation(sessionId, SetRotation.RotationModel(0, 0, z)).withId(nextId())
      .send().resBodyAs(classOf[SetRotation.Response]).value
  }

  @AutoDef
  def swipe(startX: Double, startY: Double, endX: Double, endY: Double, steps: Int): Unit = {
    swipe(null, startX, startY, endX, endY, steps)
  }

  @AutoDef
  def swipe(locator: String, startX: Double, startY: Double, endX: Double, endY: Double, steps: Int): Unit = {
    Swipe(sessionId, Swipe.SwipeModel(
      if (StringUtils.isBlank(locator)) null else elementId(locator),
      startX, startY, endX, endY, steps,
    )).withId(nextId()).send().ok()
  }

  // ms
  @AutoDef
  def longclick(x: Any, y: Any, duration: Double): Unit = {
    val pos = parseTapPoint(x, y)
    TouchLongClick(sessionId, TouchEventParams(pos._1, pos._2, duration)).withId(nextId()).send().ok()
  }

  @AutoDef
  def longclick(locator: String, duration: Double): Unit = {
    val pos = rect(locator)
    TouchLongClick(sessionId, TouchEventParams(pos.x + pos.width / 2, pos.y + pos.height / 2, duration))
      .withId(nextId()).send().ok()
  }

  @AutoDef
  def notification(): Unit = {
    OpenNotification(sessionId).withId(nextId()).send().ok()
  }

  @AutoDef
  def pressKeyCode(keycode: Int, metastate: Int, flags: Int): Unit = {
    PressKeyCode(sessionId, KeyCodeModel(keycode, metastate, flags)).withId(nextId()).send().ok()
  }

  @AutoDef
  def longPressKeyCode(keycode: Int, metastate: Int, flags: Int): Unit = {
    LongPressKeyCode(sessionId, KeyCodeModel(keycode, metastate, flags)).withId(nextId()).send().ok()
  }

  @AutoDef
  def drag(srcElId: String, destElId: String, steps: Int): Unit = {
    val body = Drag.DragModel(elementId = elementId(srcElId), destElId = elementId(destElId), steps = steps)
    Drag(sessionId, body).withId(nextId()).send().ok()
  }

  @AutoDef
  def drag(startX: Double, startY: Double, endX: Double, endY: Double, steps: Int): Unit = {
    val body = Drag.DragModel(startX = startX, startY = startY, endX = endX, endY = endY, steps = steps)
    Drag(sessionId, body).withId(nextId()).send().ok()
  }

  @AutoDef
  def flick(locator: String, xoffset: Int, yoffset: Int, speed: Int): Unit = {
    Flick(sessionId, elementId(locator), Flick.FlickByOffsetModel(xoffset, yoffset, speed))
      .withId(nextId()).send().ok()
  }

  @AutoDef
  def flick(xspeed: Int, yspeed: Int): Unit = {
    Flick(sessionId, Flick.FlickBySpeedModel(xspeed, yspeed)).withId(nextId()).send().ok()
  }

  @AutoDef
  def scrollFromTo(from: String, to: String): Unit = {
    ScrollToElement(sessionId, elementId(from), elementId(to)).withId(nextId()).send().ok()
  }

  @AutoDef
  def scrollTo(strategy: String, selector: String): Unit = {
    ScrollTo(sessionId, ScrollTo.ScrollToModel(null, ScrollTo.ScrollParams(strategy, selector)))
      .withId(nextId()).send().ok()
  }

  @AutoDef
  def scrollTo(strategy: String, selector: String, maxSwipes: Int): Unit = {
    ScrollTo(sessionId, ScrollTo.ScrollToModel(null, ScrollTo.ScrollParams(strategy, selector, maxSwipes)))
      .withId(nextId()).send().ok()
  }

  @AutoDef
  def multiPointerGesture(actions: util.Collection[util.Map[_, _]]): Unit = {
    val body = JsonUtils.mapper.convertValue(Map("actions" -> actions), classOf[MultiPointerGesture.TouchActionsModel])
    MultiPointerGesture(sessionId, body).withId(nextId()).send().ok()
  }

  def touchDown(params: TouchEventParams): Unit = {
    TouchDown(sessionId, params).withId(nextId()).send().ok()
  }

  def touchUp(params: TouchEventParams): Unit = {
    TouchUp(sessionId, params).withId(nextId()).send().ok()
  }

  def touchMove(params: TouchEventParams): Unit = {
    TouchMove(sessionId, params).withId(nextId()).send().ok()
  }

  @AutoDef
  def touch(): Mouse = IndigoMouse(this)

  @AutoDef
  def touch(locator: String): Mouse = IndigoMouse(this, locator)

  @AutoDef
  def touch(x: Int, y: Int): Mouse = IndigoMouse(this, x, y)

  @AutoDef
  def settings(settings: java.util.Map[String, Object]): Unit = {
    UpdateSettings(sessionId, settings).withId(nextId()).send().ok()
  }

  @AutoDef
  def network(): Int = {
    NetworkConnection(sessionId, NetworkConnection.NetworkConnectionModel())
      .withId(nextId()).send().resBodyAs(classOf[IntAppiumResponse]).value
  }

  @AutoDef
  def clipboard(): String = {
    val value = GetClipboard(sessionId).withId(nextId()).send().resBodyAs(classOf[GetClipboard.Response]).value
    if (value != null && value.length != 0) {
      new String(Base64.getDecoder.decode(value), StandardCharsets.UTF_8)
    } else {
      value
    }
  }

  @AutoDef
  def clipboard(content: String): Unit = {
    val c = Base64.getEncoder.encodeToString(content.getBytes(StandardCharsets.UTF_8))
    SetClipboard(sessionId, c).withId(nextId()).send().ok()
  }

  @AutoDef
  def acceptAlert(buttonLabel: String): Unit = {
    AcceptAlert(sessionId, buttonLabel).withId(nextId()).send().ok()
  }

  @AutoDef
  def dismissAlert(buttonLabel: String): Unit = {
    DismissAlert(sessionId, buttonLabel).withId(nextId()).send().ok()
  }

  def send(req: IndigoMessage): Unit = {
    if (useHub) {
      IndigoAppiumHub.write(deviceAppiumSinks, req)
    } else {
      val json = JsonUtils.stringify(req)
      logger.debug(s">> $json")
      wsClient.send(json)
    }
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
    if (useHub) {
      -1
    } else {
      id = id + 1
      id
    }
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

  def start(serial: String, sr: ScenarioRuntime): IndigoDriver = {
    val map = new util.HashMap[String, AnyRef]()
    map.put("start", Boolean.box(false)) // do not 'resolvePort' in 'DriverOptions'
    map.put("serial", serial)
    val options = new DriverOptions(map, sr, 8080, "java")
    new IndigoDriver(options, null, true)
  }

  def start(map: util.Map[String, AnyRef], sr: ScenarioRuntime): IndigoDriver = {
    map.put("start", Boolean.box(false)) // do not 'resolvePort' in 'DriverOptions'
    val options = new DriverOptions(map, sr, 8080, "java")
    var serial = map.get("serial").asInstanceOf[String]
    if (StringUtils.isBlank(serial)) {
      import FutureUtils.RichFuture
      val body = HttpUtils.getAsync(
        s"http://${options.host}:${options.port}/api/devices",
        classOf[Map[String, Any]]
      ).await(10 seconds)
      val devices = body.getOrElse("data", Nil).asInstanceOf[Seq[String]].iterator
      if (devices.hasNext) {
        serial = devices.next()
      } else {
        throw new RuntimeException("There is no device available")
      }
    }
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
    new IndigoDriver(options, url, false)
  }

}
