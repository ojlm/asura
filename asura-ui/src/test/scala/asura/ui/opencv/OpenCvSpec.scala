package asura.ui.opencv

import java.util

import asura.ui.opencv.OpencvUtils._
import asura.ui.opencv.comparator.ImageComparator
import asura.ui.opencv.comparator.ImageComparator.ComputeType
import asura.ui.opencv.detector.MSERDetector
import com.intuit.karate.driver.chrome.Chrome
import org.bytedeco.opencv.global.opencv_imgcodecs._

object OpenCvSpec {

  val driver = openDriver()

  def main(args: Array[String]): Unit = {
    testComparator()
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

  def openDriver(): Chrome = {
    val options = new util.HashMap[String, Object]()
    options.put("userDataDir", "logs/chrome")
    options.put("port", Int.box(9222))
    options.put("start", Boolean.box(false))
    val chrome = Chrome.start(options, null, true)
    chrome
  }

}
