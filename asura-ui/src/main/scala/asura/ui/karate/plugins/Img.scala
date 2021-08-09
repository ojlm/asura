package asura.ui.karate.plugins

import java.util
import java.util.Collections

import asura.common.util.StringUtils
import asura.ui.model.Position
import asura.ui.opencv.comparator.ImageComparator
import asura.ui.opencv.comparator.ImageComparator.ComputeType
import asura.ui.opencv.detector.{Detector, DetectorResult}
import asura.ui.opencv.{MatchResult, OpenCvUtils, TemplateMatch}
import com.intuit.karate.core.{AutoDef, Plugin}
import com.intuit.karate.driver.Driver
import com.typesafe.scalalogging.Logger
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.{Mat, Point}

class Img(val driver: Driver, val ocr: Ocr) extends CvPlugin {

  val templateMatch = TemplateMatch()

  @AutoDef
  def crop(): ImageElement = {
    ImageElement(null, getRootPosition(), driver, ocr, this)
  }

  @AutoDef
  def crop(locator: Any): ImageElement = {
    val parent = crop()
    val position = if (locator.isInstanceOf[String]) {
      val str = locator.asInstanceOf[String]
      if (str.charAt(0).isDigit) {
        Position(locator, parent.position)
      } else {
        Position(driver.locate(str).getPosition)
      }
    } else {
      Position(locator, parent.position)
    }
    ImageElement(parent, position, driver, ocr, this)
  }

  @AutoDef
  def crop(x: Any, y: Any): ImageElement = {
    ImageElement(crop(), Position(x, y, getRootPosition), driver, ocr, this)
  }

  @AutoDef
  def crop(x: Any, y: Any, width: Any, height: Any): ImageElement = {
    ImageElement(crop(), Position(x, y, width, height, getRootPosition), driver, ocr, this)
  }

  @AutoDef
  def click(file: String): Unit = {
    click(null, file)
  }

  @AutoDef
  def click(locator: String, file: String): Unit = {
    val target = loadBytesFromFile(file)
    if (locator == null) {
      val source = driver.screenshot(false)
      click(source, target, null)
    } else {
      val position = Position(driver.locate(locator).getPosition())
      val source = driver.screenshot(locator, false)
      click(source, target, position)
    }
  }

  @AutoDef
  def click(source: Array[Byte], target: Array[Byte]): Unit = {
    click(source, target, null)
  }

  def click(source: Array[Byte], target: Array[Byte], position: Position): Unit = {
    val result = `match`(source, target)
    if (result.regions.nonEmpty) {
      var offsetX = 0
      var offsetY = 0
      if (position != null) {
        offsetX = position.x
        offsetY = position.y
      }
      val region = result.regions(0)
      val x = (offsetX + region.x) + region.width / 2
      val y = (offsetY + region.y) + region.height / 2
      driver.mouse(x, y).click()
      drawAndEmbed(source, result.regions.map(_.toRect()), Seq(new Point(x - offsetX, y - offsetY)))
    } else {
      throw new RuntimeException("image can not found")
    }
  }

  @AutoDef
  def `match`(file: String): Unit = {
    click(null, file)
  }

  @AutoDef
  def `match`(locator: String, file: String): util.List[Element] = {
    val list = new util.ArrayList[Element]()
    val target = loadBytesFromFile(file)
    if (locator == null) {
      val source = driver.screenshot(false)
      `match`(source, target).regions.foreach(region => {
        list.add(crop(region.x, region.y, region.width, region.height))
      })
    } else {
      val position = Position(driver.locate(locator).getPosition())
      val source = driver.screenshot(locator, false)
      `match`(source, target).regions.foreach(region => {
        val regionPos = Position(region.x, region.y, region.width, region.height, position)
        list.add(ImageElement(crop(), regionPos, driver, ocr, this))
      })
    }
    list
  }

  @AutoDef
  def `match`(source: Array[Byte], file: String): MatchResult = {
    `match`(source, loadBytesFromFile(file))
  }

  @AutoDef
  def `match`(source: Array[Byte], target: Array[Byte]): MatchResult = {
    templateMatch.find(source, target, true)
  }

  @AutoDef
  def compare(file: String): Double = {
    compare(StringUtils.EMPTY, file)
  }

  @AutoDef
  def compare(locator: String, file: String): Double = {
    val fileReader = driver.getRuntime.engine.fileReader
    val target = fileReader.readFileAsBytes(file)
    val reference = if (StringUtils.isEmpty(locator)) {
      driver.screenshot(false)
    } else {
      driver.screenshot(locator, false)
    }
    compare(reference, target)
  }

  @AutoDef
  def compare(reference: Array[Byte], file: String): Double = {
    val fileReader = driver.getRuntime.engine.fileReader
    val target = fileReader.readFileAsBytes(file)
    compare(reference, target)
  }

  @AutoDef
  def compare(reference: Array[Byte], target: Array[Byte]): Double = {
    val refMat = OpenCvUtils.load(reference)
    val targetMat = OpenCvUtils.load(target)
    val tuple = OpenCvUtils.resizeToSmaller(refMat, targetMat)
    val refMatGray = new Mat()
    opencv_imgproc.cvtColor(tuple._1, refMatGray, opencv_imgproc.COLOR_BGR2GRAY)
    val comparator = ImageComparator(refMatGray, ComputeType.ssim)
    val targetMatGray = new Mat()
    opencv_imgproc.cvtColor(tuple._2, targetMatGray, opencv_imgproc.COLOR_BGR2GRAY)
    val result = comparator.compare(targetMatGray)
    embedImage(result.draw(tuple._1, tuple._2))
    result.score
  }

  @AutoDef
  def detect(): DetectorResult = {
    detect(Detector.DEFAULT)
  }

  @AutoDef
  def detect(method: String): DetectorResult = {
    detect(driver.screenshot(false), method, Collections.emptyMap[String, Any]())
  }

  @AutoDef
  def detect(locator: String, method: String, options: util.Map[String, Any]): DetectorResult = {
    detect(driver.screenshot(locator, false), method, options)
  }

  @AutoDef
  def detect(bytes: Array[Byte], method: String, options: util.Map[String, Any]): DetectorResult = {
    val result = Detector(method, options).detect(bytes, IMREAD_GRAYSCALE)
    embedImage(result.draw(bytes))
    result
  }

  @AutoDef
  def diff(file: String): Double = {
    1 - compare(file)
  }

  @AutoDef
  def diff(locator: String, file: String): Double = {
    1 - compare(locator, file)
  }

  @AutoDef
  def diff(reference: Array[Byte], target: Array[Byte]): Double = {
    1 - compare(reference, target)
  }

  def loadBytesFromFile(file: String): Array[Byte] = {
    val path = parsePath(file)
    val fileReader = driver.getRuntime.engine.fileReader
    fileReader.readFileAsBytes(path)
  }

  private def parsePath(file: String): String = {
    val parts = file.split(":")
    parts.length match {
      case 3 =>
        templateMatch.method = parts(0).toUpperCase match {
          case "SQDIFF" => opencv_imgproc.CV_TM_SQDIFF
          case "SQDIFF_NORMED" => opencv_imgproc.CV_TM_SQDIFF_NORMED
          case "CCORR" => opencv_imgproc.CV_TM_CCORR
          case "CCORR_NORMED" => opencv_imgproc.CV_TM_CCORR_NORMED
          case "CCOEFF" => opencv_imgproc.CV_TM_CCOEFF
          case "CCOEFF_NORMED" => opencv_imgproc.CV_TM_CCOEFF_NORMED
          case _ => throw new RuntimeException(s"Unknown method: ${parts(0)}. Available: ")
        }
        templateMatch.threshold = java.lang.Double.parseDouble(parts(1))
        parts(2)
      case 2 =>
        templateMatch.threshold = java.lang.Double.parseDouble(parts(0))
        parts(1)
      case 1 =>
        file
    }
  }

  override def methodNames: util.List[String] = Img.METHOD_NAMES

}

object Img {

  val logger = Logger("IMG")
  val ENGINE_KEY = "img"
  val METHOD_NAMES: util.List[String] = Plugin.methodNames(classOf[Img])

}
