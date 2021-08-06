package asura.ui.opencv.detector

import java.util

import org.bytedeco.opencv.opencv_core.{KeyPointVector, Mat}
import org.bytedeco.opencv.opencv_xfeatures2d.SURF

class SURFDetector(val options: util.Map[String, Any]) extends Detector {

  val detector = {
    if (options != null) {
      SURF.create(
        getDouble("hessianThreshold", 100),
        getInteger("nOctaves", 4),
        getInteger("nOctaveLayers", 3),
        getBoolean("extended", false),
        getBoolean("upright", false),
      )
    } else {
      SURF.create(100, 4, 3, false, false)
    }
  }

  override def detect(image: Mat): DetectorResult = {
    val keyPoints = new KeyPointVector()
    detector.detect(image, keyPoints)
    DetectorResult(pointVector = keyPoints)
  }

}

object SURFDetector {
  def apply(options: util.Map[String, Any]): SURFDetector = new SURFDetector(options)
}
