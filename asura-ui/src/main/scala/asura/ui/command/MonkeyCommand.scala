package asura.ui.command

import java.util.Random

import asura.common.util.StringUtils
import asura.ui.command.MonkeyCommand.MonkeyCommandParams
import asura.ui.driver.{CustomChromeDriver, DriverCommandLog}

case class MonkeyCommand(
                          driver: CustomChromeDriver,
                          params: MonkeyCommandParams,
                          logOut: DriverCommandLog => Unit,
                        ) extends CommandGenerator {

  import MonkeyCommand._

  var windowWidth = 0
  var windowHeight = 0
  var currentMouseXPos = 0
  var currentMouseYPos = 0

  override def init(): Unit = {
    if (StringUtils.isNotEmpty(params.startUrl)) {
      driver.setUrl(params.startUrl)
    }
    val dimensions = driver.getDimensions()
    windowWidth = dimensions.get("width").asInstanceOf[Integer]
    windowHeight = dimensions.get("height").asInstanceOf[Integer]
  }

  override def generate(): Unit = {
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
    logOut(DriverCommandLog(Commands.MONKEY, "keyboard", input))
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
    logOut(DriverCommandLog(Commands.MONKEY, "mouse", toSend.getParams()))

    toSend.send()
  }
}

object MonkeyCommand {

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
