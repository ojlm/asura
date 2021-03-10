package asura.ui.opencv.detector

import asura.ui.model.IntPoint
import asura.ui.opencv.{OpencvUtils, PointsAndImage}
import org.bytedeco.opencv.global.opencv_features2d.{NOT_DRAW_SINGLE_POINTS, drawKeypoints}
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE
import org.bytedeco.opencv.opencv_core.{KeyPointVector, Mat, Scalar}
import org.bytedeco.opencv.opencv_features2d.MSER

object MSERDetector {

  val RED_COLOR = new Scalar(0, 0, 255, 0)

  def detectKeyPoints(bytes: Array[Byte], flags: Int = IMREAD_GRAYSCALE): Seq[IntPoint] = {
    val keyPoints = getKepPointVector(bytes, flags)._2
    OpencvUtils.toPoints(keyPoints)
  }

  def detectAndGetImage(bytes: Array[Byte], flags: Int = IMREAD_GRAYSCALE): PointsAndImage = {
    val (image, keyPoints) = getKepPointVector(bytes, flags)
    val output = new Mat()
    drawKeypoints(image, keyPoints, output, RED_COLOR, NOT_DRAW_SINGLE_POINTS)
    PointsAndImage(OpencvUtils.toPoints(keyPoints), OpencvUtils.toBytes(output))
  }

  private def getKepPointVector(bytes: Array[Byte], flags: Int = IMREAD_GRAYSCALE): (Mat, KeyPointVector) = {
    val image = OpencvUtils.load(bytes, flags)
    // FIXME: create a 'MSER' every time?
    val mser = MSER.create()
    val keyPoints = new KeyPointVector()
    mser.detect(image, keyPoints)
    (image, keyPoints)
  }

}
