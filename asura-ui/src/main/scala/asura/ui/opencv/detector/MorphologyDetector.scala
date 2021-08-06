package asura.ui.opencv.detector

import java.util

import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc.{Sobel, _}
import org.bytedeco.opencv.opencv_core.{Mat, _}

class MorphologyDetector(val options: util.Map[String, Any]) extends Detector {

  var shape = MORPH_RECT
  var width = 17
  var height = 3
  var op = CV_MOP_CLOSE
  var mode = CV_RETR_EXTERNAL
  var method = CV_CHAIN_APPROX_NONE

  if (options != null) {
    shape = getString("shape", "RECT").toUpperCase match {
      case "RECT" => MORPH_RECT
      case "CROSS" => MORPH_CROSS
      case "ELLIPSE" => MORPH_ELLIPSE
      case _ => throw new RuntimeException("'shape' must be one of: RECT,CROSS,ELLIPSE")
    }
    width = getInteger("width", 17)
    height = getInteger("height", 3)
    op = getString("op", "CLOSE").toUpperCase match {
      case "CLOSE" => CV_MOP_CLOSE
      case "ERODE" => CV_MOP_ERODE
      case "DILATE" => CV_MOP_DILATE
      case "OPEN" => CV_MOP_OPEN
      case "GRADIENT" => CV_MOP_GRADIENT
      case "TOPHAT" => CV_MOP_TOPHAT
      case "BLACKHAT" => CV_MOP_BLACKHAT
      case _ => throw new RuntimeException("'shape' must be one of: CLOSE,ERODE,DILATE,OPEN,GRADIENT,TOPHAT,BLACKHAT")
    }
    mode = getString("mode", "EXTERNAL").toUpperCase match {
      case "EXTERNAL" => RETR_EXTERNAL
      case "LIST" => RETR_LIST
      case "CCOMP" => RETR_CCOMP
      case "TREE" => RETR_TREE
      case "FLOODFILL" => RETR_FLOODFILL
      case _ => throw new RuntimeException("'mode' must be one of: EXTERNAL,LIST,CCOMP,TREE,FLOODFILL")
    }
    method = getString("method", "NONE").toUpperCase match {
      case "NONE" => CHAIN_APPROX_NONE
      case "SIMPLE" => CHAIN_APPROX_SIMPLE
      case "TC89_L1" => CHAIN_APPROX_TC89_L1
      case "TC89_KCOS" => CHAIN_APPROX_TC89_KCOS
      case _ => throw new RuntimeException("'method' must be one of: NONE,SIMPLE,TC89_L1,TC89_KCOS")
    }
  }

  override def detect(image: Mat): DetectorResult = {
    Sobel(image, image, CV_8U, 1, 0, 3, 1, 0, BORDER_DEFAULT)
    threshold(image, image, 0, 255, CV_THRESH_OTSU + CV_THRESH_BINARY)
    val element = getStructuringElement(shape, new Size(width, height))
    morphologyEx(image, image, op, element)
    val contours = new MatVector()
    findContours(image, contours, mode, method)
    DetectorResult(contours = contours)
  }

}

object MorphologyDetector {
  def apply(options: util.Map[String, Any]): MorphologyDetector = new MorphologyDetector(options)
}
