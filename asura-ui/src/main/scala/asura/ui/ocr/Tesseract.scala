package asura.ui.ocr

import java.io.File
import java.nio.charset.StandardCharsets

import scala.collection.mutable.ArrayBuffer

import asura.common.util.ResourceUtils
import asura.ui.ocr.Tesseract.{Level, logger}
import asura.ui.opencv.OpenCvUtils
import com.typesafe.scalalogging.Logger
import org.bytedeco.javacpp.{BytePointer, IntPointer, Pointer, PointerPointer}
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.tesseract.global.tesseract
import org.bytedeco.tesseract.{FileReader, StringGenericVector, TessBaseAPI}

class Tesseract(val api: TessBaseAPI, confidenceThreshold: Int = 50) {

  def process(src: Mat, negative: Boolean = false, level: String = Level.WORD): Words = {
    val levelValue = level match {
      case Level.BLOCK => tesseract.RIL_BLOCK
      case Level.PARA => tesseract.RIL_PARA
      case Level.LINE => tesseract.RIL_TEXTLINE
      case Level.WORD => tesseract.RIL_WORD
      case Level.SYMBOL => tesseract.RIL_SYMBOL
      case _ => tesseract.RIL_WORD
    }
    val mat = if (negative) OpenCvUtils.negative(src) else src
    val size = mat.size()
    val width = size.width()
    val height = size.height()
    val channels = mat.channels()
    val bytesPerLine = width * channels * mat.elemSize1().toInt
    api.SetImage(mat.data().asBuffer(), width, height, channels, bytesPerLine)
    val textPtr = api.GetUTF8Text()
    val allText = textPtr.getString(StandardCharsets.UTF_8)
    textPtr.deallocate()
    val ri = api.GetIterator()
    val words = ArrayBuffer[Word]()
    var prev: Word = null
    do {
      val confidence = ri.Confidence(levelValue)
      if (confidence >= confidenceThreshold) {
        val pointer = ri.GetUTF8Text(levelValue)
        val text = pointer.getString(StandardCharsets.UTF_8).trim()
        pointer.deallocate()
        val left = new IntPointer(1L)
        val top = new IntPointer(1L)
        val right = new IntPointer(1L)
        val bottom = new IntPointer(1L)
        val found = ri.BoundingBox(levelValue, left, top, right, bottom)
        val x = left.get()
        val y = top.get()
        val width = right.get() - x
        val height = bottom.get() - y
        if (found) {
          val word = Word(text, x, y, width, height, confidence)
          word.prev = prev
          words += word
          prev = word
        } else {
          logger.warn(s"no such rectangle: $x:$y:$width:$height")
        }
      }
    } while (ri.Next(levelValue))
    Words(allText, words.toSeq)
  }

}

object Tesseract {

  val logger = Logger("Tesseract")

  object Level {
    val BLOCK = "block"
    val PARA = "para"
    val LINE = "line"
    val WORD = "word"
    val SYMBOL = "symbol"
  }

  def apply(): Tesseract = {
    val bytes = ResourceUtils.getAsBytes("tessdata/chi_sim.traineddata")
    apply("chi_sim+~chi_sim_vert", bytes)
  }

  def apply(path: String, lang: String): Tesseract = {
    val api = new TessBaseAPI()
    if (api.Init(path, lang) != 0) {
      throw new RuntimeException(s"tesseract init failed: $path,$lang")
    } else {
      new Tesseract(api)
    }
  }

  def apply(file: File, lang: String): Tesseract = {
    apply(file.getPath, lang)
  }

  def apply(lang: String, bytes: Array[Byte]): Tesseract = {
    val api = new TessBaseAPI()
    val nullPtr = new PointerPointer[Pointer]()
    val varsVector = new StringGenericVector()
    val data = new BytePointer(bytes: _*)
    val language = new BytePointer(lang, StandardCharsets.UTF_8)
    val code = api.Init(data, bytes.length, language,
      tesseract.OEM_DEFAULT, nullPtr, 0,
      varsVector, varsVector,
      false, new FileReader(null)
    )
    if (code != 0) {
      throw new RuntimeException(s"tesseract init failed, ${lang}")
    } else {
      new Tesseract(api)
    }
  }

}
