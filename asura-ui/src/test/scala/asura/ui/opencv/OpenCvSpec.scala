package asura.ui.opencv

import java.util

import asura.ui.opencv.OpencvUtils._
import asura.ui.opencv.detector.MSERDetector
import com.intuit.karate.driver.chrome.Chrome
import org.bytedeco.opencv.global.opencv_imgcodecs._

object OpenCvSpec {

  def main(args: Array[String]): Unit = {
    val options = new util.HashMap[String, Object]()
    options.put("start", Boolean.box(false))
    val driver = Chrome.start(options, null, true)
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
