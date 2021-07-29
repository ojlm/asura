package asura.ui.opencv.compute

import java.text.NumberFormat

import asura.ui.opencv.OpenCvUtils._
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

case class Result(score: Double, result: Mat = null) {

  def resultMat(reference: Mat, target: Mat): Mat = {
    val text = {
      val format = NumberFormat.getPercentInstance()
      format.setMaximumFractionDigits(2)
      s"similarity: ${format.format(score)}"
    }
    if (result == null) {
      drawLineOnImage(target, new Point(0, 0), new Point(0, target.rows()), Colors.BGR_GREEN)
      val dst = concat(reference, target)
      drawTextOnImage(dst, text, Colors.BGR_RED)
      dst
    } else { // ssim
      result.mul(result, 255)
      result.convertTo(result, CV_8UC1)
      threshold(result, result, score * 255, 255, THRESH_BINARY_INV | THRESH_OTSU)
      val mid = new Mat()
      if (score < 1) {
        bitwise_not(result, mid)
        cvtColor(mid, mid, COLOR_GRAY2BGR)
      } else {
        cvtColor(result, mid, COLOR_GRAY2BGR)
      }
      val contours = new MatVector()
      findContours(result, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)
      contours.get().foreach(contour => {
        drawRectOnImage(target, boundingRect(contour), Colors.BGR_RED)
      })
      drawLineOnImage(mid, new Point(0, 0), new Point(0, mid.rows()), Colors.BGR_GREEN)
      drawLineOnImage(mid, new Point(mid.cols() - 1, 0), new Point(mid.cols() - 1, mid.rows()), Colors.BGR_GREEN)
      drawTextOnImage(mid, text, Colors.BGR_RED)
      concat(reference, mid, target)
    }
  }

}
