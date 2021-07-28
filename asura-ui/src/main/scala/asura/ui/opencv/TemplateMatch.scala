package asura.ui.opencv

import scala.collection.mutable.ArrayBuffer

import com.typesafe.scalalogging.Logger
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * https://docs.opencv.org/2.4/modules/imgproc/doc/object_detection.html
 * https://stackoverflow.com/questions/48799711/explain-difference-between-opencvs-template-matching-methods-in-non-mathematica
 * https://stackoverflow.com/questions/32041063/multiple-template-matching-only-detects-one-match
 */
case class TemplateMatch(
                          var resize: Boolean = true,
                          var threshold: Double = 0.8,
                          var method: Int = CV_TM_CCOEFF_NORMED
                        ) {

  def find(source: Array[Byte], target: Array[Byte], findAll: Boolean): MatchResult = {
    TemplateMatch.find(
      OpenCvUtils.load(source, IMREAD_GRAYSCALE), OpenCvUtils.load(target, IMREAD_GRAYSCALE),
      findAll, threshold, resize, method,
    )
  }

  def find(source: Mat, target: Mat, findAll: Boolean): MatchResult = {
    TemplateMatch.find(source, target, findAll, threshold, resize, method)
  }

}

object TemplateMatch {

  val logger = Logger("TemplateMatch")

  def find(source: Mat, target: Mat, findAll: Boolean, threshold: Double, resize: Boolean, method: Int): MatchResult = {
    val regions = ArrayBuffer[Region]()
    var point = find(regions, source, target, findAll, threshold, 1, method)
    if (regions.isEmpty && resize) {
      val stepUp = find(regions, source, target, findAll, threshold, 1.1, method)
      if (regions.isEmpty) {
        val stepDown = find(regions, source, target, findAll, threshold, 0.9, method)
        if (regions.isEmpty) {
          val goUpFirst = stepUp.minVal < stepDown.minVal
          for (step <- 2 until 6) {
            val scale = 1 + 0.1 * step * (if (goUpFirst) 1 else -1)
            find(regions, source, target, findAll, threshold, scale, method)
          }
          if (regions.isEmpty) {
            for (step <- 2 until 6) {
              val scale = 1 + 0.1 * step * (if (goUpFirst) -1 else 1)
              find(regions, source, target, findAll, threshold, scale, method)
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

  def find(
            regions: ArrayBuffer[Region], source: Mat, target: Mat,
            findAll: Boolean, threshold: Double, scale: Double, method: Int
          ): MatchPoint = {
    val targetWidth = target.cols()
    val targetHeight = target.rows()
    val result = new Mat()
    val point = getBestMatch(source, target, result, scale, method)
    if (point.hasMatch(threshold)) {
      logger.debug(s"found match at scale $scale, minVal: ${point.minVal} / $threshold at ${point.minX}:${point.minY}")
      if (!findAll) {
        regions += point.toRegion(targetWidth, targetHeight)
      } else {
        addRegionsInThreshold(regions, result, threshold, scale, targetWidth, targetHeight, method)
      }
    } else {
      logger.debug(s"no match at scale $scale, minVal: ${point.minVal} / $threshold at ${point.minX}:${point.minY}")
    }
    point
  }

  def addRegionsInThreshold(
                             regions: ArrayBuffer[Region], result: Mat, threshold: Double,
                             scale: Double, width: Int, height: Int, method: Int, span: Int = 2,
                           ): Unit = {
    val dst = new Mat()
    val useMin = if (method == CV_TM_SQDIFF || method == CV_TM_SQDIFF_NORMED) {
      opencv_imgproc.threshold(result, dst, threshold, 1, CV_THRESH_TOZERO_INV)
      true
    } else {
      opencv_imgproc.threshold(result, dst, threshold, 1, CV_THRESH_TOZERO)
      false
    }
    val regionWidth = Math.round(width / scale).toInt
    val regionHeight = Math.round(height / scale).toInt
    val minValPtr = new DoublePointer(1)
    val maxValPtr = new DoublePointer(1)
    val minPt = new Point()
    val maxPt = new Point()
    var hasMore = true
    val zero = new Scalar(0)
    val add = (x: Int, y: Int) => {
      regions += Region(
        x = Math.round(x / scale).toInt,
        y = Math.round(y / scale).toInt,
        width = regionWidth, height = regionHeight,
      )
      rectangle(dst, new Rect(x - span, y - span, regionWidth + span, regionHeight + span), zero, -1, LINE_8, 0)
    }
    do {
      minMaxLoc(dst, minValPtr, maxValPtr, minPt, maxPt, null)
      if (useMin && minValPtr.get() > 0) {
        add(minPt.x(), minPt.y())
      } else if (!useMin && maxValPtr.get() >= threshold) {
        add(maxPt.x(), maxPt.y())
      } else {
        hasMore = false
      }
    } while (hasMore)
  }

  def getBestMatch(source: Mat, target: Mat, result: Mat, scale: Double = 1, method: Int): MatchPoint = {
    val resized = if (scale == 1) source else OpenCvUtils.rescale(source, scale)
    matchTemplate(resized, target, result, method)
    val minValPtr = new DoublePointer(1)
    val maxValPtr = new DoublePointer(1)
    val minPt = new Point()
    val maxPt = new Point()
    minMaxLoc(result, minValPtr, maxValPtr, minPt, maxPt, null)
    MatchPoint(
      method = method, scale = scale,
      minVal = minValPtr.get(), minX = minPt.x(), minY = minPt.y(),
      maxVal = maxValPtr.get(), maxX = maxPt.x(), maxY = maxPt.y(),
    )
  }

}
