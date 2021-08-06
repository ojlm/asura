package asura.ui.opencv.detector

import java.util

import asura.ui.opencv.OpenCvUtils._
import org.bytedeco.opencv.opencv_core.Mat

trait Detector {

  val options: util.Map[String, Any]

  def detect(image: Mat): DetectorResult

  def detect(bytes: Array[Byte], flags: Int): DetectorResult = {
    detect(load(bytes, flags))
  }

  def getString(key: String, default: String): String = {
    val value = options.get(key)
    if (value != null) {
      value match {
        case v: String => v
        case _ => throw new RuntimeException(s"Need a string value: $key")
      }
    } else {
      default
    }
  }

  def getBoolean(key: String, default: Boolean): Boolean = {
    val value = options.get(key)
    if (value != null) {
      value match {
        case v: Boolean => v
        case v: String => java.lang.Boolean.parseBoolean(v)
        case _ => throw new RuntimeException(s"Need a boolean value (true or false): $key")
      }
    } else {
      default
    }
  }

  def getInteger(key: String, default: Integer): Integer = {
    val value = options.get(key)
    if (value != null) {
      value match {
        case v: Integer => v
        case v: Float => v.toInt
        case v: Double => v.toInt
        case v: String => Integer.parseInt(v)
        case _ => throw new RuntimeException(s"Need an integer value: $key")
      }
    } else {
      default
    }
  }

  def getDouble(key: String, default: Double): Double = {
    val value = options.get(key)
    if (value != null) {
      value match {
        case v: Integer => v.toDouble
        case v: Float => v.toDouble
        case v: Double => v
        case v: String => java.lang.Double.parseDouble(v)
        case _ => throw new RuntimeException(s"Need a double value: $key")
      }
    } else {
      default
    }
  }

}

object Detector {

  val Morph = "Morph".toLowerCase
  val Harris = "Harris".toLowerCase
  val MSER = "MSER".toLowerCase
  val GFTT = "GFTT".toLowerCase
  val FAST = "FAST".toLowerCase
  val SURF = "SURF".toLowerCase
  val SIFT = "SIFT".toLowerCase
  val DEFAULT = Morph

  def apply(method: String, options: util.Map[String, Any]): Detector = {
    method.toLowerCase match {
      case Morph => MorphologyDetector(options)
      case Harris => HarrisDetector(options)
      case MSER => MSERDetector(options)
      case GFTT => GfttDetector(options)
      case FAST => FastDetector(options)
      case SURF => SURFDetector(options)
      case SIFT => SIFTDetector(options)
      case _ => throw new RuntimeException(s"Unknown method $method")
    }
  }

}
