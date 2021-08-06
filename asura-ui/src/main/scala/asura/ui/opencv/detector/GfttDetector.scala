package asura.ui.opencv.detector

import java.util

import org.bytedeco.opencv.opencv_core.{KeyPointVector, Mat}
import org.bytedeco.opencv.opencv_features2d._

class GfttDetector(val options: util.Map[String, Any]) extends Detector {

  val detector = {
    if (options != null) {
      GFTTDetector.create(
        getInteger("maxCorners", 500),
        getDouble("qualityLevel", 0.01),
        getDouble("minDistance", 10.0),
        getInteger("blockSize", 3),
        getBoolean("useHarris", false),
        getDouble("k", 0.04),
      )
    } else {
      GFTTDetector.create(500, 0.01, 10.0, 3, false, 0.04)
    }
  }

  override def detect(image: Mat): DetectorResult = {
    val keyPoints = new KeyPointVector()
    detector.detect(image, keyPoints)
    DetectorResult(pointVector = keyPoints)
  }

}

object GfttDetector {
  def apply(options: util.Map[String, Any]): GfttDetector = new GfttDetector(options)
}
