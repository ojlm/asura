package asura.ui.opencv.detector

import asura.ui.opencv.OpenCvUtils._
import org.bytedeco.opencv.global.opencv_features2d.{NOT_DRAW_SINGLE_POINTS, drawKeypoints}
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import org.bytedeco.opencv.global.opencv_imgproc.{approxPolyDP, boundingRect, rectangle}
import org.bytedeco.opencv.opencv_core._

case class DetectorResult(
                           pointVector: KeyPointVector = null,
                           points: Seq[Point] = null,
                           rects: Array[Rect] = null,
                           contours: MatVector = null,
                         ) {

  def draw(image: Mat): Mat = {
    if (pointVector != null && !pointVector.isNull) {
      drawKeypoints(image, pointVector, image, Colors.Red, NOT_DRAW_SINGLE_POINTS)
    }
    if (points != null) {
      points.foreach(point => drawPointOnImage(image, point, 4, Colors.Red))
    }
    if (rects != null) {
      rects.foreach(rect => {
        rectangle(image, rect, Colors.Red)
      })
    }
    if (contours != null) {
      contours.get().foreach(contour => {
        val curve = new Mat()
        approxPolyDP(contour, curve, 3, true)
        val rect = boundingRect(curve)
        rectangle(image, rect, Colors.Red)
      })
    }
    image
  }

  def draw(reference: Array[Byte], flags: Int = IMREAD_COLOR): Mat = {
    draw(load(reference, flags))
  }

}
