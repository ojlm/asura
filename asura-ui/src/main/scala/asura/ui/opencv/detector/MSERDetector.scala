package asura.ui.opencv.detector

import java.util

import asura.ui.model.IntPoint
import asura.ui.opencv.OpenCvUtils.Colors
import asura.ui.opencv.{OpenCvUtils, PointsAndImage}
import org.bytedeco.opencv.global.opencv_features2d.{NOT_DRAW_SINGLE_POINTS, drawKeypoints}
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE
import org.bytedeco.opencv.opencv_core.{KeyPointVector, Mat}
import org.bytedeco.opencv.opencv_features2d.MSER

class MSERDetector(val options: util.Map[String, Any]) extends Detector {

  val detector = {
    if (options != null) {
      MSER.create(
        getInteger("delta", 5),
        getInteger("minArea", 60),
        getInteger("maxArea", 14400),
        getDouble("maxVariation", 0.25),
        getDouble("minDiversity", 0.2),
        getInteger("maxEvolution", 200),
        getDouble("areaThreshold", 1.01),
        getDouble("minMargin", 0.003),
        getInteger("edgeBlurSize", 5),
      )
    } else {
      MSER.create()
    }
  }

  override def detect(image: Mat): DetectorResult = {
    val keyPoints = new KeyPointVector()
    detector.detect(image, keyPoints)
    // val msers = new PointVectorVector()
    // val bboxes = new RectVector()
    // mser.detectRegions(image, msers, bboxes)
    DetectorResult(pointVector = keyPoints)
  }

}

object MSERDetector {

  def apply(options: util.Map[String, Any]): MSERDetector = new MSERDetector(options)

  def detectKeyPoints(bytes: Array[Byte], flags: Int = IMREAD_GRAYSCALE): Seq[IntPoint] = {
    val keyPoints = getKepPointVector(bytes, flags)._2
    OpenCvUtils.toPoints(keyPoints)
  }

  def detectAndGetImage(bytes: Array[Byte], flags: Int = IMREAD_GRAYSCALE): PointsAndImage = {
    val (image, keyPoints) = getKepPointVector(bytes, flags)
    val output = new Mat()
    drawKeypoints(image, keyPoints, output, Colors.Red, NOT_DRAW_SINGLE_POINTS)
    PointsAndImage(OpenCvUtils.toPoints(keyPoints), OpenCvUtils.toBytes(output))
  }

  private def getKepPointVector(bytes: Array[Byte], flags: Int = IMREAD_GRAYSCALE): (Mat, KeyPointVector) = {
    val image = OpenCvUtils.load(bytes, flags)
    val mser = MSER.create()
    val keyPoints = new KeyPointVector()
    mser.detect(image, keyPoints)
    (image, keyPoints)
  }

}
