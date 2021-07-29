package asura.ui.karate.plugins

import java.util

import asura.ui.model.Position
import asura.ui.opencv.comparator.ImageComparator
import asura.ui.opencv.comparator.ImageComparator.ComputeType
import asura.ui.opencv.{OpenCvUtils, TemplateMatch}
import com.intuit.karate.core.{AutoDef, Plugin}
import com.intuit.karate.driver.Driver
import com.typesafe.scalalogging.Logger
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.{Mat, Point}

class Img(val driver: Driver) extends CvPlugin {

  val templateMatch = TemplateMatch()

  @AutoDef
  def click(file: String): Unit = {
    click(null, file)
  }

  @AutoDef
  def click(locator: String, file: String): Unit = {
    val path = parsePath(file)
    val fileReader = driver.getRuntime.engine.fileReader
    val target = fileReader.readFileAsBytes(path)
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
    val result = templateMatch.find(source, target, false)
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
  def compare(file: String): Double = {
    compare(null, file)
  }

  @AutoDef
  def compare(locator: String, file: String): Double = {
    val fileReader = driver.getRuntime.engine.fileReader
    val target = fileReader.readFileAsBytes(file)
    val reference = if (locator == null) {
      driver.screenshot(false)
    } else {
      driver.screenshot(locator, false)
    }
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
    drawAndEmbed(result.resultMat(tuple._1, tuple._2))
    result.score
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
