package asura.ui.opencv

import asura.common.util.JsonUtils
import asura.ui.BaseSpec
import asura.ui.opencv.OpenCvUtils._
import asura.ui.opencv.comparator.ImageComparator
import asura.ui.opencv.comparator.ImageComparator.ComputeType
import asura.ui.opencv.detector.MSERDetector
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

object OpenCvSpec extends BaseSpec {

  val driver = openDriver()

  def main(args: Array[String]): Unit = {
    testComparator()
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
    val result = template.find(source, target, true)
    println(s"point: ${JsonUtils.stringifyPretty(result.point)}")
    println(s"matched: ${JsonUtils.stringifyPretty(result.regions)}")
    if (result.regions.nonEmpty) {
      result.regions.foreach(region => {
        drawRectOnImage(colored, region.toRect(), Colors.Red)
      })
      show(colored, "colored")
    }
  }

  def testComparator(): Unit = {
    val referenceBytes = driver.screenshot(false)
    val referenceColor = load(referenceBytes)
    val reference = load(referenceBytes, IMREAD_GRAYSCALE)
    val comparator = ImageComparator(reference, ComputeType.ssim)
    show(referenceColor, "reference")
    val secondBytes = driver.screenshot(false)
    val second = load(secondBytes, IMREAD_GRAYSCALE)
    val secondColor = load(secondBytes)
    val result = comparator.compare(second)
    val ssim = result.result
    printInfo(ssim, "ssim:")
    ssim.mul(ssim, 255)
    ssim.convertTo(ssim, CV_8UC1)
    threshold(ssim, ssim, result.score * 255, 255, THRESH_BINARY_INV | THRESH_OTSU)
    if (result.score < 1) {
      val inv = new Mat()
      bitwise_not(ssim, inv)
      show(inv, "ssim")
    } else {
      show(ssim, "ssim")
    }
    val counters = new MatVector()
    findContours(ssim, counters, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)
    println(s"counters: ${counters.size()}")
    counters.get().foreach(cnt => {
      val rect = boundingRect(cnt)
      drawRectOnImage(secondColor, rect, Colors.Red)
      println(s"x: ${rect.x()}, y: ${rect.y()}, w: ${rect.width()}, h: ${rect.height()}")
    })
    show(secondColor, "score: %6.4f".format(result.score))
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
