package asura.ui.opencv.comparator

import asura.ui.opencv.OpencvUtils
import asura.ui.opencv.comparator.ImageComparator.ComputeType
import asura.ui.opencv.compute.{ColorHistogram, Compute, Result, SSIM}
import org.bytedeco.opencv.opencv_core.Mat

class ImageComparator(reference: Mat, computeType: String = ComputeType.ssim) {

  private val computeFunc: Compute = computeType match {
    case ComputeType.histogram => new ColorHistogram(256)
    case ComputeType.ssim => new SSIM()
    case _ => new SSIM()
  }
  private val referenceValue: Mat = computeType match {
    case ComputeType.histogram => computeFunc.compute(reference)
    case ComputeType.ssim => reference
    case _ => reference
  }

  def compare(image: Mat): Result = computeFunc.compare(referenceValue, image)

  def compare(image: Array[Byte]): Result = {
    compare(OpencvUtils.load(image))
  }

}

object ImageComparator {

  def apply(referenceImage: Mat, computeType: String): ImageComparator = {
    new ImageComparator(referenceImage, computeType)
  }

  def apply(referenceImage: Array[Byte], computeType: String): ImageComparator = {
    new ImageComparator(OpencvUtils.load(referenceImage), computeType)
  }

  object ComputeType {
    val histogram = "histogram"
    val ssim = "SSIM"
  }

}
