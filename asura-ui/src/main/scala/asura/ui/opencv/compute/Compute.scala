package asura.ui.opencv.compute

import org.bytedeco.opencv.opencv_core.Mat

trait Compute {
  def compute(image: Mat): Mat

  def compare(reference: Mat, second: Mat): Result
}
