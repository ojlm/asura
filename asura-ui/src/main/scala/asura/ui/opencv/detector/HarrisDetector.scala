package asura.ui.opencv.detector

import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

class HarrisDetector(
                      var neighborhood: Int = 3,
                      var aperture: Int = 3,
                      var k: Double = 0.01,
                      var maxStrength: Double = 0.0,
                      var nonMaxSize: Int = 3,
                    ) {

  private var cornerStrength: Option[Mat] = None
  private var localMax: Option[Mat] = None

  def detect(image: Mat): Unit = {
    cornerStrength = Some(new Mat())
    cornerHarris(image, cornerStrength.get, neighborhood, aperture, k)

    // Internal threshold computation.
    //
    // We will scale corner threshold based on the maximum value in the cornerStrength image.
    // Call to cvMinMaxLoc finds min and max values in the image and assigns them to output parameters.
    // Passing back values through function parameter pointers works in C bout not on JVM.
    // We need to pass them as 1 element array, as a work around for pointers in C API.
    val maxStrengthA = new DoublePointer(maxStrength)
    minMaxLoc(
      cornerStrength.get,
      new DoublePointer(0.0) /* not used here, but required by API */ ,
      maxStrengthA, null, null, new Mat())
    // Read back the computed maxStrength
    maxStrength = maxStrengthA.get(0)

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


  /** Get the corner map from the computed Harris values. Require call to `detect`.
   *
   * @throws IllegalStateException if `cornerStrength` and `localMax` are not yet computed.
   */
  def getCornerMap(qualityLevel: Double): Mat = {
    if (cornerStrength.isEmpty || localMax.isEmpty) {
      throw new IllegalStateException("Need to call `detect()` before it is possible to compute corner map.")
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
  def getCorners(qualityLevel: Double = 0.01): List[Point] = {
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

  /**
   * Draw circles at feature point locations on an image
   */
  def drawOnImage(image: Mat, points: List[Point]): Unit = {
    val radius = 4
    val thickness = 1
    val color = new Scalar(255, 255, 255, 0)
    points.foreach { p => circle(image, new Point(p.x, p.y), radius, color, thickness, 8, 0) }
  }

}
