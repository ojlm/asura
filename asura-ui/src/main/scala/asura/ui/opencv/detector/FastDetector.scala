package asura.ui.opencv.detector

import java.util

import org.bytedeco.opencv.opencv_core.{KeyPointVector, Mat}
import org.bytedeco.opencv.opencv_features2d._

class FastDetector(val options: util.Map[String, Any]) extends Detector {

  val detector = {
    if (options != null) {
      FastFeatureDetector.create(
        getInteger("threshold", 10),
        getBoolean("nonmaxSuppression", true),
        getInteger("type", FastFeatureDetector.TYPE_9_16),
      )
    } else {
      FastFeatureDetector.create(10, true, FastFeatureDetector.TYPE_9_16)
    }
  }

  override def detect(image: Mat): DetectorResult = {
    val keyPoints = new KeyPointVector()
    detector.detect(image, keyPoints)
    DetectorResult(pointVector = keyPoints)
  }

}

object FastDetector {
  def apply(options: util.Map[String, Any]): FastDetector = new FastDetector(options)
}



