package asura.ui.opencv

import asura.common.util.JsonUtils
import asura.ui.BaseSpec
import asura.ui.opencv.OpenCvUtils._
import asura.ui.opencv.comparator.ImageComparator
import asura.ui.opencv.comparator.ImageComparator.ComputeType
import asura.ui.opencv.detector.MSERDetector
import org.bytedeco.opencv.global.opencv_imgcodecs._

object OpenCvSpec extends BaseSpec {

  val driver = openDriver()

  def main(args: Array[String]): Unit = {
    testTemplateMatch()
  }

  def testTemplateMatch(): Unit = {
    val template = TemplateMatch()
    val colored = loadFilePath("asura-ui/src/test/scala/asura/ui/opencv/full.png")
    val source = loadFilePath("asura-ui/src/test/scala/asura/ui/opencv/full.png", IMREAD_GRAYSCALE)
    printInfo(source, "source")
    show(source, "source")
    val target = loadFilePath("asura-ui/src/test/scala/asura/ui/opencv/target.png", IMREAD_GRAYSCALE)
    printInfo(target, "target")
    show(target, "target")
    val result = template.find(source, target, false)
    println(s"point: ${JsonUtils.stringifyPretty(result.point)}")
    println(s"matched: ${JsonUtils.stringifyPretty(result.regions)}")
    if (result.regions.nonEmpty) {
      result.regions.foreach(region => {
        drawOnImage(colored, region.toRect(), Colors.BGR_RED)
      })
      show(colored, "colored")
    }
  }

  def testComparator(): Unit = {
    val reference = load(driver.screenshot(false))
    printInfo(reference, "reference:")
    val comparator = ImageComparator(reference, ComputeType.ssim)
    show(reference, "reference")
    val second = load(driver.screenshot(false))
    val score = comparator.compare(second)
    show(second, "score: %6.4f".format(score.score))
    val result = score.result
    printInfo(result, "ssim:")
  }

  def testMSERDetector(): Unit = {
    val bytes = driver.screenshot(false)
    val image = load(bytes, IMREAD_GRAYSCALE)
    show(image, "origin screenshot")
    val res = MSERDetector.detectAndGetImage(bytes)
    val points = res.points
    println(points.length)
    points.foreach(p => {
      println(s"x: ${p.x}, y: ${p.y}")
    })
    show(load(res.image), "rendered image")
  }

}
