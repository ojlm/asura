package asura.ui.command

import java.util
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorRef
import asura.common.util.{JsonUtils, StringUtils}
import asura.ui.driver.{CustomChromeDriver, DriverCommand, DriverCommandEnd, DriverCommandLog}
import asura.ui.karate.KarateRunner
import asura.ui.model.Position
import asura.ui.util.{RandomStringUtils, TypeConverters}

case class MonkeyCommandRunner(
                                driver: CustomChromeDriver,
                                command: DriverCommand,
                                stopNow: AtomicBoolean,
                                logActor: ActorRef,
                              ) extends CommandRunner {

  import MonkeyCommandRunner._

  implicit val actions = KarateRunner.buildStepActions(driver)
  val params = JsonUtils.mapper.convertValue(command.params, classOf[MonkeyCommandParams])
  val areaRatio = new util.TreeMap[Float, Position]()

  val fullWindowRect = Position(0, 0, 0, 0)
  var currentMouseXPos = 0
  var currentMouseYPos = 0

  var checkIntervalInMs = 0L
  var lastCheck = System.currentTimeMillis()

  private def setupWindow(): Unit = {
    if (StringUtils.isNotEmpty(params.startUrl)) {
      driver.setUrl(params.startUrl)
    }
    val dimensions = driver.getDimensions()
    fullWindowRect.width = dimensions.get("width").asInstanceOf[Integer]
    fullWindowRect.height = dimensions.get("height").asInstanceOf[Integer]
    if (params.areaRatio != null) {
      var totalRatio = 0f
      params.areaRatio
        .filter(item => item.ratio > 0f && item.ratio <= 1f && StringUtils.isNotEmpty(item.locator))
        .foreach(item => {
          val position = driver.position(item.locator)
          if (position != null && totalRatio <= 1f) {
            totalRatio = totalRatio + item.ratio
            val posObj = Position(
              TypeConverters.toInt(position.get("x")),
              TypeConverters.toInt(position.get("y")),
              TypeConverters.toInt(position.get("width")),
              TypeConverters.toInt(position.get("height")),
            )
            if (null != logActor) {
              logActor ! DriverCommandLog(Commands.MONKEY, "log", s"position(${item.locator}): ${posObj.x},${posObj.y},${posObj.width},${posObj.height}")
            }
            areaRatio.put(totalRatio, posObj)
          }
        })
    }
    if (params.checkInterval > 0) {
      checkIntervalInMs = params.checkInterval * 1000
    }
    if (null != logActor) {
      logActor ! DriverCommandLog(Commands.MONKEY, "log", s"window size: ${fullWindowRect.width}*${fullWindowRect.height}")
    }
    if (StringUtils.isNotEmpty(params.beforeScript)) {
      params.beforeScript.lines().forEach(step => runStep(step))
    }
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
          sleepAndRunIntervalCheck()
          i += 1
        }
      } else {
        while (checkDuration()) {
          generateOnce()
          sleepAndRunIntervalCheck()
        }
      }
      DriverCommandEnd(Commands.MONKEY, true)
    }
  }

  private def sleepAndRunIntervalCheck(): Unit = {
    Thread.sleep(params.interval) // ugly
    if (
      checkIntervalInMs > 0 &&
        StringUtils.isNotEmpty(params.checkScript) &&
        (System.currentTimeMillis() - lastCheck) > checkIntervalInMs
    ) {
      params.checkScript.lines().forEach(step => runStep(step))
      lastCheck = System.currentTimeMillis()
    }
  }

  private def runStep(step: String): Unit = {
    val stepResult = KarateRunner.executeStep(step)
    if (null != logActor) {
      logActor ! DriverCommandLog(Commands.MONKEY, "log", s"before step: $step, result: ${stepResult.status}")
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
    // val count = RANDOM.nextInt(params.maxOnceKeyCount) + 1
    // val charArray = new Array[Char](count)
    // for (i <- 0 until count) {
    //   charArray(i) = CHARS(RANDOM.nextInt(CHARS.length))
    // }
    // val input = String.valueOf(charArray)
    // driver.input(input)
    val input = RandomStringUtils.nextString(params.minOnceKeyCount, params.maxOnceKeyCount, params.cjkRatio)
    if (null != logActor) logActor ! DriverCommandLog(Commands.MONKEY, "keyboard", input)
    driver.input(input)
  }

  private def generateMouseEvent(): Unit = {
    val mouseEventType = MOUSE_TYPES(RANDOM.nextInt(MOUSE_TYPES.length))
    val toSend = driver.method("Input.dispatchMouseEvent").param("type", mouseEventType)
    if ("mousePressed" == mouseEventType || "mouseReleased" == mouseEventType) {
      val button = MOUSE_BUTTONS(RANDOM.nextInt(MOUSE_BUTTONS.length))
      toSend.param("button", button).param("clickCount", 1)
    } else if ("mouseMoved" == mouseEventType) {
      var rect = fullWindowRect
      if (areaRatio.size() > 0) {
        val entry = areaRatio.ceilingEntry(RANDOM.nextFloat())
        if (entry != null) rect = entry.getValue()
      }
      currentMouseXPos = rect.x + RANDOM.nextInt(rect.width)
      currentMouseYPos = rect.y + RANDOM.nextInt(rect.height)
    } else if ("mouseWheel" == mouseEventType) { // mouseWheel
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
                                  var minOnceKeyCount: Int = 1,
                                  var maxOnceKeyCount: Int = 5,
                                  var cjkRatio: Float = 1f,
                                  var keyEventRatio: Float = 0.7f,
                                  var interval: Int = 500, // ms
                                  var generateCount: Int = 100,
                                  var maxDuration: Int = 0, // second
                                  var beforeScript: String = null,
                                  var checkInterval: Int = 0, // second
                                  var checkScript: String = null,
                                  var areaRatio: Seq[AreaRatio] = null,
                                ) {

    def validate(): Unit = {
      if (delta <= 0) delta = 100
      if (minOnceKeyCount <= 0) minOnceKeyCount = 1
      if (maxOnceKeyCount <= 0 || maxOnceKeyCount < minOnceKeyCount) maxOnceKeyCount = minOnceKeyCount
      if (keyEventRatio <= 0 || keyEventRatio > 1f) keyEventRatio = 0.7f
      if (cjkRatio <= 0 || cjkRatio > 1f) cjkRatio = 1f
      if (interval <= 0) interval = 500
      if (generateCount < 0) generateCount = 0
      if (maxDuration < 0) maxDuration = 0
    }

  }

  case class AreaRatio(locator: String, ratio: Float)

}
