package asura.ui.opencv.detector

import java.util

import org.bytedeco.opencv.opencv_core.{KeyPointVector, Mat}
import org.bytedeco.opencv.opencv_features2d.SIFT

class SIFTDetector(val options: util.Map[String, Any]) extends Detector {

  val detector = {
    if (options != null) {
      SIFT.create(
        getInteger("nFeatures", 0),
        getInteger("nOctaveLayers", 3),
        getDouble("contrastThreshold", 0.04),
        getDouble("edgeThreshold", 10),
        getDouble("sigma", 1.6),
      )
    } else {
      SIFT.create(0, 3, 0.04, 10, 1.6)
    }
  }

  override def detect(image: Mat): DetectorResult = {
    val keyPoints = new KeyPointVector()
    detector.detect(image, keyPoints)
    DetectorResult(pointVector = keyPoints)
  }

}

object SIFTDetector {
  def apply(options: util.Map[String, Any]): SIFTDetector = new SIFTDetector(options)
}
