package asura.ui.opencv.detector

import java.util

import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

class HarrisDetector(val options: util.Map[String, Any]) extends Detector {

  var neighborhood: Int = 3 // Neighborhood size for Harris edge detector
  var aperture: Int = 3 // Aperture size for Harris edge detector
  var k: Double = 0.01 // Harris detector free parameter
  var qualityLevel: Double = 0.01

  if (options != null) {
    neighborhood = getInteger("neighborhood", 3)
    aperture = getInteger("aperture", 3)
    k = getDouble("k", 0.01)
    qualityLevel = getDouble("qualityLevel", 0.01)
  }

  // Image of corner strength, computed by Harris edge detector
  private var cornerStrength: Option[Mat] = None
  // Image of local corner maxima
  private var localMax: Option[Mat] = None

  // Maximum strength for threshold computations
  var maxStrength: Double = 0.0

  override def detect(image: Mat): DetectorResult = {
    doDetect(image)
    DetectorResult(points = getCorners())
  }

  def doDetect(image: Mat): Unit = {
    cornerStrength = Some(new Mat())
    cornerHarris(image, cornerStrength.get, neighborhood, aperture, k)

    // Internal threshold computation.
    //
    // We will scale corner threshold based on the maximum value in the cornerStrength image.
    // Call to cvMinMaxLoc finds min and max values in the image and assigns them to output parameters.
    // Passing back values through function parameter pointers works in C bout not on JVM.
    // We need to pass them as 1 element array, as a work around for pointers in C API.
    val maxStrengthPointer = new DoublePointer(maxStrength)
    minMaxLoc(
      cornerStrength.get,
      new DoublePointer(0.0) /* not used here, but required by API */ ,
      maxStrengthPointer, null, null, new Mat())
    // Read back the computed maxStrength
    maxStrength = maxStrengthPointer.get(0)

    // Local maxima detection.
    //
    // Dilation will replace values in the image by its largest neighbour value.
    // This process will modify all the pixels but the local maxima (and plateaus)
    val dilated = new Mat()
    dilate(cornerStrength.get, dilated, new Mat())
    localMax = Some(new Mat())
    // Find maxima by detecting which pixels were not modified by dilation
    compare(cornerStrength.get, dilated, localMax.get, CMP_EQ)
  }


  /** Get the corner map from the computed Harris values. Require call to `doDetect`. */
  def getCornerMap(qualityLevel: Double): Mat = {
    if (cornerStrength.isEmpty || localMax.isEmpty) {
      throw new IllegalStateException("Need to call `doDetect()` before it is possible to compute corner map.")
    }
    // Threshold the corner strength
    val t = qualityLevel * maxStrength
    val cornerTh = new Mat()
    threshold(cornerStrength.get, cornerTh, t, 255, THRESH_BINARY)
    val cornerMap = new Mat()
    cornerTh.convertTo(cornerMap, CV_8U)
    // non-maxima suppression
    bitwise_and(cornerMap, localMax.get, cornerMap)
    cornerMap
  }

  /** Get the feature points from the computed Harris values. Require call to `detect`. */
  def getCorners(qualityLevel: Double = qualityLevel): List[Point] = {
    // Get the corner map
    val cornerMap = getCornerMap(qualityLevel)
    // Get the corners
    getCorners(cornerMap)
  }

  /** Get the feature points vector from the computed corner map. */
  private def getCorners(cornerMap: Mat): List[Point] = {
    val i = cornerMap.createIndexer[UByteIndexer]()
    // Iterate over the pixels to obtain all feature points where matrix has non-zero values
    val width = cornerMap.cols
    val height = cornerMap.rows
    val points = for (y <- 0 until height; x <- 0 until width if i.get(y, x) != 0) yield new Point(x, y)
    points.toList
  }

}

object HarrisDetector {
  def apply(options: util.Map[String, Any]): HarrisDetector = new HarrisDetector(options)
}
