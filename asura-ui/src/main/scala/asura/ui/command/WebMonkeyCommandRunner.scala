package asura.ui.command

import java.util
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorRef
import asura.common.util.StringUtils
import asura.ui.command.WebMonkeyCommandRunner.MonkeyCommandParams
import asura.ui.driver._
import asura.ui.karate.KarateRunner
import asura.ui.model.{BytesObject, IntPoint, Position}
import asura.ui.opencv.detector.MSERDetector
import asura.ui.util.RandomStringUtils
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer

case class WebMonkeyCommandRunner(
                                   driver: CustomChromeDriver,
                                   meta: CommandMeta,
                                   params: MonkeyCommandParams,
                                   stopNow: AtomicBoolean,
                                   logActor: ActorRef,
                                   electron: Boolean,
                                 ) extends CommandRunner {

  import WebMonkeyCommandRunner._

  implicit val actions = KarateRunner.buildStepActions(driver)
  val areaRatioPositionMap = new util.TreeMap[Float, Position]()
  val areaRatioLocatorMap = new util.TreeMap[Float, String]()
  val areaLocatorPointsMap = new util.HashMap[String, Seq[IntPoint]]()
  var fullScreenPoints: Seq[IntPoint] = Nil
  val excludeArea = ArrayBuffer[Position]()
  var mouseButtonsLength = MOUSE_BUTTONS.length

  val fullWindowRect = Position(0, 0, 0, 0)
  var currentMouseXPos = 0
  var currentMouseYPos = 0

  var checkIntervalInMs = 0L
  var lastCheck = System.currentTimeMillis()

  private def setupWindow(): Unit = {
    if (StringUtils.isNotEmpty(params.startUrl)) {
      driver.setUrl(params.startUrl)
    }
    if (StringUtils.isNotEmpty(params.beforeScript)) {
      params.beforeScript.lines().forEach(step => runStep(step))
    }
    val dimensions = if (electron) {
      driver.position("body")
    } else {
      driver.getDimensions()
    }
    fullWindowRect.width = dimensions.get("width").asInstanceOf[Integer]
    fullWindowRect.height = dimensions.get("height").asInstanceOf[Integer]
    if (params.areaRatio != null) {
      var totalRatio = 0f
      params.areaRatio
        .filter(item => item.ratio > 0f && item.ratio <= 1f && StringUtils.isNotEmpty(item.locator))
        .foreach(item => {
          try {
            if (totalRatio <= 1f) {
              totalRatio = totalRatio + item.ratio
              val position = driver.position(item.locator)
              val posObj = Position(position)
              if (logActor != null) {
                logActor ! DriverCommandLog(Commands.WEB_MONKEY, DriverCommandLog.TYPE_LOG, s"position(${item.locator}): ${posObj.x},${posObj.y},${posObj.width},${posObj.height}", meta)
              }
              areaRatioPositionMap.put(totalRatio, posObj)
              if (item.useCvDetectPoints) {
                areaRatioLocatorMap.put(totalRatio, item.locator)
                val bytes = driver.screenshot(item.locator)
                val image = MSERDetector.detectAndGetImage(bytes)
                if (image.points != null && image.points.nonEmpty) {
                  image.points.foreach(p => p.offset(posObj.x, posObj.y))
                  areaLocatorPointsMap.put(item.locator, image.points)
                  if (logActor != null) {
                    logActor ! DriverCommandLog(Commands.WEB_MONKEY, DriverCommandLog.TYPE_SCREEN, BytesObject(item.locator, image.image), meta)
                  }
                } else {
                  throwError(s"Points of ${item.locator} is empty")
                }
              }
            }
          } catch {
            case _: Throwable =>
              throwError(s"Can't get position/points of locator:'${item.locator}'")
          }
        })
    }
    if (params.useCvDetectPoints) {
      val bytes = driver.screenshot()
      val image = MSERDetector.detectAndGetImage(bytes)
      if (image.points != null && image.points.nonEmpty) {
        fullScreenPoints = image.points
        if (logActor != null) {
          logActor ! DriverCommandLog(Commands.WEB_MONKEY, DriverCommandLog.TYPE_SCREEN, BytesObject("", image.image), meta)
        }
      } else {
        throwError("Points of screen is empty")
      }
    }
    if (params.excludeArea != null) {
      params.excludeArea
        .filter(item => StringUtils.isNotEmpty(item.locator))
        .foreach(item => {
          try {
            val position = driver.position(item.locator)
            if (position != null) {
              excludeArea += Position(position)
            }
          } catch {
            case _: Throwable => throwError(s"Can't get position/points of locator:'${item.locator}'")
          }
        })
    }
    if (params.disableMouseRightKey) mouseButtonsLength = MOUSE_BUTTONS.length - 1
    if (params.checkInterval > 0) {
      checkIntervalInMs = params.checkInterval * 1000
    }
    if (logActor != null) {
      logActor ! DriverCommandLog(Commands.WEB_MONKEY, DriverCommandLog.TYPE_LOG, s"window size: ${fullWindowRect.width}*${fullWindowRect.height}", meta)
    }
  }

  def throwError(msg: String): Unit = {
    logger.error(msg)
    throw new RuntimeException(msg)
  }

  override def run(): DriverCommandEnd = {
    params.validate()
    if (params.generateCount == 0 && params.maxDuration == 0) {
      DriverCommandEnd(Commands.WEB_MONKEY, true)
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
      DriverCommandEnd(Commands.WEB_MONKEY, true)
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
    if (logActor != null) {
      logActor ! DriverCommandLog(Commands.WEB_MONKEY, DriverCommandLog.TYPE_LOG, s"before step: $step, result: ${stepResult.status}", meta)
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
    var inputStr = RandomStringUtils.nextString(params.minOnceKeyCount, params.maxOnceKeyCount, params.cjkRatio)
    if (StringUtils.isNotEmpty(params.excludeChars)) {
      val sb = new StringBuilder()
      inputStr.foreach(c => {
        if (!params.excludeChars.contains(c)) sb.append(c)
      })
      inputStr = sb.toString()
    }
    if (null != logActor) logActor ! DriverCommandLog(Commands.WEB_MONKEY, DriverCommandLog.TYPE_KEYBOARD, inputStr, meta)
    driver.input(inputStr)
  }

  private def generateMouseEvent(): Unit = {
    val mouseEventType = MOUSE_TYPES(RANDOM.nextInt(MOUSE_TYPES.length))
    val toSend = driver.method("Input.dispatchMouseEvent").param("type", mouseEventType)
    if ("mousePressed" == mouseEventType || "mouseReleased" == mouseEventType) {
      val button = MOUSE_BUTTONS(RANDOM.nextInt(mouseButtonsLength))
      toSend.param("button", button).param("clickCount", 1)
    } else if ("mouseMoved" == mouseEventType) {
      val newPos = if (areaLocatorPointsMap.isEmpty && fullScreenPoints.isEmpty) {
        var rect = fullWindowRect
        if (areaRatioPositionMap.size() > 0) {
          val entry = areaRatioPositionMap.ceilingEntry(RANDOM.nextFloat())
          if (entry != null) rect = entry.getValue()
        }
        rect.randomPoint(RANDOM)
      } else { // use cv detected points
        var points: Seq[IntPoint] = fullScreenPoints
        if (!areaLocatorPointsMap.isEmpty) {
          val entry = areaRatioLocatorMap.ceilingEntry(RANDOM.nextFloat())
          if (entry != null) {
            points = areaLocatorPointsMap.get(entry.getValue)
          }
        }
        if (points.nonEmpty) {
          val point = points(RANDOM.nextInt(points.length))
          (point.x, point.y)
        } else {
          fullWindowRect.randomPoint(RANDOM)
        }
      }
      if (excludeArea.nonEmpty) {
        if (excludeArea.find(p => p.inArea(newPos._1, newPos._2)).isEmpty) {
          currentMouseXPos = newPos._1
          currentMouseYPos = newPos._2
        }
      } else {
        currentMouseXPos = newPos._1
        currentMouseYPos = newPos._2
      }
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
    if (null != logActor) logActor ! DriverCommandLog(Commands.WEB_MONKEY, DriverCommandLog.TYPE_MOUSE, toSend.getParams(), meta)
    toSend.send()
  }
}

object WebMonkeyCommandRunner {

  val logger = Logger("WebMonkeyCommandRunner")
  val RANDOM = new Random()
  val MOUSE_TYPES: Array[String] = Array[String]("mousePressed", "mouseReleased", "mouseMoved", "mouseWheel")
  val MOUSE_BUTTONS: Array[String] = Array[String]("none", "left", "middle", "right")
  var CHARS: Array[Character] = CustomChromeDriver.CODES.keySet().toArray(Array[Character]())

  case class MonkeyCommandParams(
                                  var startUrl: String = null,
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
                                  var excludeArea: Seq[AreaRatio] = null,
                                  var excludeChars: String = null,
                                  var disableMouseRightKey: Boolean = false,
                                  var useCvDetectPoints: Boolean = false,
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

  case class AreaRatio(locator: String, ratio: Float, useCvDetectPoints: Boolean = false)

}
