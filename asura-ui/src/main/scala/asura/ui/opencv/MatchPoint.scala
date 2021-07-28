package asura.ui.opencv

import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core.Rect

case class MatchPoint(
                       method: Int,
                       scale: Double,
                       minVal: Double,
                       minX: Int,
                       minY: Int,
                       maxVal: Double,
                       maxX: Int,
                       maxY: Int,
                     ) {

  // 'xx_NORMED' method is used
  def hasMatch(threshold: Double): Boolean = {
    if (method == CV_TM_SQDIFF || method == CV_TM_SQDIFF_NORMED) {
      minVal <= threshold
    } else {
      maxVal >= threshold
    }
  }

  def matchRect(width: Int, height: Int): Rect = {
    if (method == CV_TM_SQDIFF || method == CV_TM_SQDIFF_NORMED) {
      new Rect(minX, minY, width, height)
    } else {
      new Rect(maxX, maxY, width, height)
    }
  }

  def toRegion(width: Int, height: Int): Region = {
    var bestX = minX
    var bestY = minY
    if (method != CV_TM_SQDIFF && method != CV_TM_SQDIFF_NORMED) {
      bestX = maxX
      bestY = maxY
    }
    Region(
      x = Math.round(bestX / scale).toInt,
      y = Math.round(bestY / scale).toInt,
      width = Math.round(width / scale).toInt,
      height = Math.round(height / scale).toInt,
    )
  }

}
