package asura.ui.opencv.compute

import java.nio.{FloatBuffer, IntBuffer}

import asura.ui.opencv.OpenCvUtils
import org.bytedeco.javacpp.{FloatPointer, IntPointer, PointerPointer}
import org.bytedeco.opencv.global.opencv_core.split
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core.{Mat, MatVector}

/**
 * Helper class that simplifies usage of OpenCV `calcHist` function for color images.
 */
class ColorHistogram(
                      var numberOfBins: Int = 256,
                      minRange: Float = 0.0f,
                      maxRange: Float = 255.0f
                    ) extends Compute {

  override def compute(image: Mat): Mat = getHistogram(image)

  override def compare(reference: Mat, second: Mat): Result = {
    val secondValue = compute(second)
    val imageSize = second.cols() * second.rows()
    Result(compareHist(reference, secondValue, HISTCMP_INTERSECT) / imageSize, null)
  }

  /**
   * Computes histogram of an image.
   *
   * @param image input image
   * @return OpenCV histogram object
   */
  def getHistogram(image: Mat): Mat = {
    require(image != null)
    require(image.channels == 3, "Expecting 3 channel (color) image")
    // Compute histogram
    val hist = new Mat()
    // Since C++ `calcHist` is using arrays of arrays we need wrap to do some wrapping
    // in `IntPointer` and `PointerPointer` objects.
    val intPtrChannels = new IntPointer(0, 1, 2)
    val intPtrHistSize = new IntPointer(numberOfBins, numberOfBins, numberOfBins)
    val histRange = Array(minRange, maxRange)
    val ptrPtrHistRange = new PointerPointer[FloatPointer](histRange, histRange, histRange)
    calcHist(image,
      1, // histogram of 1 image only
      intPtrChannels, // the channel used
      new Mat(), // no mask is used
      hist, // the resulting histogram
      3, // it is a 3D histogram
      intPtrHistSize, // number of bins
      ptrPtrHistRange, // pixel value range
      true, // uniform
      false) // no accumulation
    hist
  }

  /**
   * Convert input image from RGB ro HSV color space and compute histogram of the hue channel.
   *
   * @param image         RGB image
   * @param minSaturation minimum saturation of pixels that are used for histogram calculations.
   *                      Pixels with saturation larger than minimum will be used in histogram computation
   * @return histogram of the hue channel, its range is from 0 to 180.
   */
  def getHueHistogram(image: Mat, minSaturation: Int = 0): Mat = {
    require(image != null)
    require(image.channels == 3, "Expecting 3 channel (color) image")
    // Convert RGB to HSV color space
    val hsvImage = new Mat()
    cvtColor(image, hsvImage, COLOR_BGR2HSV)
    val saturationMask = new Mat()
    if (minSaturation > 0) {
      // Split the 3 channels into 3 images
      val hsvChannels = new MatVector()
      split(hsvImage, hsvChannels)
      threshold(hsvChannels.get(1), saturationMask, minSaturation, 255, THRESH_BINARY)
    }
    // Prepare arguments for a 1D hue histogram
    val hist = new Mat()
    // range is from 0 to 180
    val histRanges = FloatBuffer.wrap(Array(0f, 180f))
    // the hue channel
    val channels = IntBuffer.wrap(Array(0))
    // Compute histogram
    calcHist(hsvImage,
      1, // histogram of 1 image only
      channels, // the channel used
      saturationMask, // binary mask
      hist, // the resulting histogram
      1, // it is a 1D histogram
      OpenCvUtils.wrapInIntBuffer(numberOfBins), // number of bins
      histRanges // pixel value range
    )
    hist
  }

  /**
   * Computes the 2D ab histogram. BGR source image is converted to Lab
   */
  def getabHistogram(image: Mat): Mat = {
    val hist = new Mat()
    // Convert to Lab color space
    val lab = new Mat()
    cvtColor(image, lab, COLOR_BGR2Lab)
    // Prepare arguments for a 2D color histogram
    val histRange = Array(0f, 255f)
    val ptrPtrHistRange = new PointerPointer[FloatPointer](histRange, histRange, histRange)
    // the two channels used are ab
    val intPtrChannels = new IntPointer(1, 2)
    val intPtrHistSize = new IntPointer(numberOfBins, numberOfBins)
    // Compute histogram
    calcHist(lab,
      1, // histogram of 1 image only
      intPtrChannels, // the channel used
      new Mat(), // no mask is used
      hist, // the resulting histogram
      2, // it is a 2D histogram
      intPtrHistSize, // number of bins
      ptrPtrHistRange, // pixel value range
      true, // Uniform
      false // do not accumulate
    )
    hist
  }

}
