package asura.ui.command

import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorRef
import asura.common.util.{JsonUtils, StringUtils}
import asura.ui.driver.{CustomChromeDriver, DriverCommand, DriverCommandEnd, DriverCommandLog}

case class MonkeyCommandRunner(
                                driver: CustomChromeDriver,
                                command: DriverCommand,
                                stopNow: AtomicBoolean,
                                logActor: ActorRef,
                              ) extends CommandRunner {

  import MonkeyCommandRunner._

  var windowWidth = 0
  var windowHeight = 0
  var currentMouseXPos = 0
  var currentMouseYPos = 0

  val params = JsonUtils.mapper.convertValue(command.params, classOf[MonkeyCommandParams])

  private def setupWindow(): Unit = {
    if (StringUtils.isNotEmpty(params.startUrl)) {
      driver.setUrl(params.startUrl)
    }
    val dimensions = driver.getDimensions()
    windowWidth = dimensions.get("width").asInstanceOf[Integer]
    windowHeight = dimensions.get("height").asInstanceOf[Integer]
  }

  override def run(): DriverCommandEnd = {
    params.validate()
    if (params.generateCount == 0 && params.maxDuration == 0) {
      DriverCommandEnd(Commands.MONKEY, true)
    } else {
      setupWindow()
      val start = System.currentTimeMillis()
      val durationInMs = params.maxDuration * 1000
      val checkDuration = () => {
        if (stopNow.get()) {
          stopNow.set(false)
          false
        } else {
          if (durationInMs > 0) {
            ((System.currentTimeMillis() - start) < durationInMs)
          } else {
            true
          }
        }
      }
      if (params.generateCount > 0) {
        var i = 0
        while (i < params.generateCount && checkDuration()) {
          generateOnce()
          Thread.sleep(params.interval) // ugly
          i += 1
        }
      } else {
        while (checkDuration()) {
          generateOnce()
          Thread.sleep(params.interval) // ugly
        }
      }
      DriverCommandEnd(Commands.MONKEY, true)
    }
  }

  private def generateOnce(): Unit = {
    if (params.keyEventRatio == 1f) {
      generateKeyEvent()
    } else if (params.keyEventRatio == 0f) {
      generateMouseEvent()
    } else {
      if (RANDOM.nextFloat() <= params.keyEventRatio) {
        generateKeyEvent()
      } else {
        generateMouseEvent()
      }
    }
  }

  private def generateKeyEvent(): Unit = {
    val count = RANDOM.nextInt(params.maxOnceKeyCount) + 1
    val charArray = new Array[Char](count)
    for (i <- 0 until count) {
      charArray(i) = CHARS(RANDOM.nextInt(CHARS.length))
    }
    val input = String.valueOf(charArray)
    if (null != logActor) logActor ! DriverCommandLog(Commands.MONKEY, "keyboard", input)
    driver.input(input)
  }

  private def generateMouseEvent(): Unit = {
    val mouseEventType = MOUSE_TYPES(RANDOM.nextInt(MOUSE_TYPES.length))
    val toSend = driver.method("Input.dispatchMouseEvent").param("type", mouseEventType)
    if ("mousePressed" == mouseEventType || "mouseReleased" == mouseEventType) {
      val button = MOUSE_BUTTONS(RANDOM.nextInt(MOUSE_BUTTONS.length))
      toSend.param("button", button).param("clickCount", 1)
    }
    else if ("mouseMoved" == mouseEventType) {
      currentMouseXPos = RANDOM.nextInt(windowWidth)
      currentMouseYPos = RANDOM.nextInt(windowHeight)
    }
    else if ("mouseWheel" == mouseEventType) { // mouseWheel
      var delta = params.delta
      if (RANDOM.nextFloat() > 0.5) {
        delta = -delta
      }
      if (RANDOM.nextFloat() > 0.5) {
        toSend.param("deltaX", 0)
        toSend.param("deltaY", delta)
      } else {
        toSend.param("deltaX", delta)
        toSend.param("deltaY", 0)
      }
    }
    toSend.param("x", currentMouseXPos).param("y", currentMouseYPos)
    if (null != logActor) logActor ! DriverCommandLog(Commands.MONKEY, "mouse", toSend.getParams())
    toSend.send()
  }
}

object MonkeyCommandRunner {

  val RANDOM = new Random()
  val MOUSE_TYPES: Array[String] = Array[String]("mousePressed", "mouseReleased", "mouseMoved", "mouseWheel")
  val MOUSE_BUTTONS: Array[String] = Array[String]("none", "left", "middle", "right")
  var CHARS: Array[Character] = CustomChromeDriver.CODES.keySet().toArray(Array[Character]())

  case class MonkeyCommandParams(
                                  var startUrl: String,
                                  var delta: Int = 100,
                                  var maxOnceKeyCount: Int = 5,
                                  var keyEventRatio: Float = 0.7f,
                                  var interval: Int = 500, // ms
                                  var generateCount: Int = 100,
                                  var maxDuration: Int = 0, // s
                                ) {

    def validate(): Unit = {
      if (delta <= 0) delta = 100
      if (maxOnceKeyCount <= 0) maxOnceKeyCount = 5
      if (keyEventRatio <= 0 || keyEventRatio > 1f) keyEventRatio = 0.7f
      if (interval <= 0) interval = 500
      if (generateCount < 0) generateCount = 0
      if (maxDuration < 0) maxDuration = 0
    }
  }

}
