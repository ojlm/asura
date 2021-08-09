package asura.ui.opencv.detector

import scala.collection.mutable.ArrayBuffer

import asura.ui.opencv.OpenCvUtils._
import asura.ui.opencv.Region
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

  private var parsedRegions: ArrayBuffer[Region] = null

  def regions(): Seq[Region] = {
    if (parsedRegions == null) {
      parsedRegions = ArrayBuffer[Region]()
      if (rects != null || contours != null) {
        if (rects != null) {
          rects.foreach(rect => parsedRegions += Region(rect))
        }
        if (contours != null) {
          contours.get().foreach(contour => {
            val curve = new Mat()
            approxPolyDP(contour, curve, 3, true)
            val rect = boundingRect(curve)
            parsedRegions += Region(rect)
          })
        }
      }
    }
    parsedRegions.toSeq
  }

  def draw(image: Mat): Mat = {
    if (pointVector != null && !pointVector.isNull) {
      drawKeypoints(image, pointVector, image, Colors.Red, NOT_DRAW_SINGLE_POINTS)
    }
    if (points != null) {
      points.foreach(point => drawPointOnImage(image, point, 4, Colors.Red))
    }
    if (parsedRegions == null) {
      if (rects != null || contours != null) {
        parsedRegions = ArrayBuffer[Region]()
        if (rects != null) {
          rects.foreach(rect => {
            parsedRegions += Region(rect)
            rectangle(image, rect, Colors.Red)
          })
        }
        if (contours != null) {
          contours.get().foreach(contour => {
            val curve = new Mat()
            approxPolyDP(contour, curve, 3, true)
            val rect = boundingRect(curve)
            parsedRegions += Region(rect)
            rectangle(image, rect, Colors.Red)
          })
        }
      }
    } else {
      parsedRegions.foreach(region => rectangle(image, region.toRect(), Colors.Red))
    }
    image
  }

  def draw(reference: Array[Byte], flags: Int = IMREAD_COLOR): Mat = {
    draw(load(reference, flags))
  }

}
