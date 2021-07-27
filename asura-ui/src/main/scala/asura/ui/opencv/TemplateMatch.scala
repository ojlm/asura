package asura.ui.opencv

import scala.collection.mutable.ArrayBuffer

import com.typesafe.scalalogging.Logger
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

case class TemplateMatch(var strictness: Int = 10, var resize: Boolean = true) {

  def find(source: Array[Byte], target: Array[Byte], findAll: Boolean): MatchResult = {
    TemplateMatch.find(
      OpenCvUtils.load(source, IMREAD_GRAYSCALE), OpenCvUtils.load(target, IMREAD_GRAYSCALE),
      findAll, strictness, resize,
    )
  }

  def find(source: Mat, target: Mat, findAll: Boolean): MatchResult = {
    TemplateMatch.find(source, target, findAll, strictness, resize)
  }

}

object TemplateMatch {

  val logger = Logger("TemplateMatch")
  val TARGET_MIN_VAL_FACTOR = 150
  val BLOCK_SIZE = 5

  def find(source: Mat, target: Mat, findAll: Boolean, strictness: Int, resize: Boolean): MatchResult = {
    val regions = ArrayBuffer[Region]()
    var point = find(regions, source, target, findAll, strictness, 1)
    if (regions.isEmpty && resize) {
      val stepUp = find(regions, source, target, findAll, strictness, 1.1)
      if (regions.isEmpty) {
        val stepDown = find(regions, source, target, findAll, strictness, 0.9)
        if (regions.isEmpty) {
          val goUpFirst = stepUp.minVal < stepDown.minVal
          for (step <- 2 until 6) {
            val scale = 1 + 0.1 * step * (if (goUpFirst) 1 else -1)
            find(regions, source, target, findAll, strictness, scale)
          }
          if (regions.isEmpty) {
            for (step <- 2 until 6) {
              val scale = 1 + 0.1 * step * (if (goUpFirst) -1 else 1)
              find(regions, source, target, findAll, strictness, scale)
            }
          }
        } else {
          point = stepDown
        }
      } else {
        point = stepUp
      }
    }
    MatchResult(regions.toSeq, point)
  }

  def find(regions: ArrayBuffer[Region], source: Mat, target: Mat, findAll: Boolean, strictness: Int, scale: Double): MatchPoint = {
    val targetWidth = target.cols()
    val targetHeight = target.rows()
    val targetMinVal = targetWidth * targetHeight * TARGET_MIN_VAL_FACTOR * strictness
    val result = new Mat()
    val point = doMatch(source, target, result, scale)
    if (point.minVal > targetMinVal) {
      logger.debug(s"no match at scale $scale, minVal: ${point.minVal} / $targetMinVal at ${point.minX}:${point.minY}")
    } else {
      logger.debug(s"found match at scale $scale, minVal: ${point.minVal} / $targetMinVal at ${point.minX}:${point.minY}")
      if (findAll) {
        addRegionsBelowThreshold(regions, result, targetMinVal, scale, targetWidth, targetHeight)
      } else {
        regions += point.toRegion(targetWidth, targetHeight)
      }
    }
    point
  }

  def addRegionsBelowThreshold(
                                regions: ArrayBuffer[Region], src: Mat, threshold: Double,
                                scale: Double, width: Int, height: Int,
                              ): Unit = {
    val dst = new Mat()
    opencv_imgproc.threshold(src, dst, threshold, 1, CV_THRESH_BINARY_INV)
    val regionWidth = Math.round(width / scale).toInt
    val regionHeight = Math.round(height / scale).toInt
    val non = new Mat()
    findNonZero(dst, non)
    val len = non.total().toInt
    var xPrev = -BLOCK_SIZE
    var yPrev = -BLOCK_SIZE
    var countPrev = 0
    var xSum = 0
    var ySum = 0
    val checkNeedAdd: () => Unit = () => {
      if (countPrev > 0) {
        val xFinal = Math.floorDiv(xSum, countPrev)
        val yFinal = Math.floorDiv(ySum, countPrev)
        regions += Region(
          Math.round(xFinal / scale).toInt,
          Math.round(yFinal / scale).toInt,
          regionWidth, regionHeight,
        )
      }
    }
    for (i <- 0 until len) {
      val point = new Point(non.ptr(i))
      val x = point.x()
      val y = point.y()
      val xDelta = Math.abs(x - xPrev)
      val yDelta = Math.abs(y - yPrev)
      if (xDelta < BLOCK_SIZE && yDelta < BLOCK_SIZE) {
        countPrev = countPrev + 1
        xSum = xSum + x
        ySum = ySum + y
      } else {
        checkNeedAdd()
        xSum = xSum + x
        ySum = ySum + y
        countPrev = 1
      }
      xPrev = x
      yPrev = y
    }
    checkNeedAdd()
  }

  def doMatch(source: Mat, target: Mat, result: Mat, scale: Double = 1): MatchPoint = {
    val resized = if (scale == 1) source else OpenCvUtils.rescale(source, scale)
    matchTemplate(resized, target, result, CV_TM_SQDIFF)
    val minValPtr = new DoublePointer(1)
    val maxValPtr = new DoublePointer(1)
    val minPt = new Point()
    val maxPt = new Point()
    minMaxLoc(result, minValPtr, maxValPtr, minPt, maxPt, null)
    MatchPoint(
      scale = scale,
      minVal = minValPtr.get().toInt, minX = minPt.x(), minY = minPt.y(),
      maxVal = maxValPtr.get().toInt, maxX = maxPt.x(), maxY = maxPt.y(),
    )
  }

}
