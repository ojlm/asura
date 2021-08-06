package asura.ui.ocr

import asura.ui.BaseSpec
import asura.ui.ocr.Tesseract.Level
import asura.ui.opencv.OpenCvUtils._
import org.bytedeco.opencv.global.opencv_imgcodecs._

object TesseractSpec extends BaseSpec {

  val driver = openDriver()
  val dataPath = "asura-ui/src/main/resources/tessdata"

  def main(args: Array[String]): Unit = {
    val bytes = driver.screenshot(false)
    val origin = load(bytes)
    val src = load(bytes, IMREAD_GRAYSCALE)
    show(src, "src")
    // val engine = Tesseract(dataPath, "chi_sim")
    val engine = Tesseract()
    val words = engine.process(src, level = Level.WORD)
    println(words)
    words.words.foreach(word => {
      drawRectOnImage(origin, word.toRect(), Colors.Red)
    })
    show(origin, "target")
  }

}
