package asura.ui.karate.plugins

import java.util

import asura.ui.karate.plugins.Ocr.Pair
import asura.ui.model.Position
import asura.ui.ocr.Tesseract
import asura.ui.ocr.Tesseract.Level
import asura.ui.opencv.OpenCvUtils._
import com.intuit.karate.core.{AutoDef, Plugin}
import com.intuit.karate.driver.Driver
import com.typesafe.scalalogging.Logger
import org.bytedeco.opencv.global.opencv_imgcodecs._

class Ocr(val driver: Driver) extends CvPlugin {

  private var tess: Tesseract = null

  @AutoDef
  def click(text: String): Unit = {
    click(null, text)
  }

  @AutoDef
  def click(locator: String, text: String): Unit = {
    init()
    val pair = parseOcr(text)
    val negative = if (pair != null && pair.left.charAt(0) == '-') {
      true
    } else {
      false
    }
    var offsetX = 0
    var offsetY = 0
    val bytes = if (locator == null) {
      driver.screenshot(false)
    } else {
      val position = Position(driver.locate(locator).getPosition())
      offsetX = position.x
      offsetY = position.y
      driver.screenshot(locator, false)
    }
    val result = tess.find(load(bytes, IMREAD_GRAYSCALE), text, negative)
    if (result.found.nonEmpty) {
      val word = result.found(0)
      driver.mouse(offsetX + word.x, offsetY + word.y).click()
      drawAndEmbed(bytes, result.found.map(_.toRect()))
    } else {
      drawAndEmbed(bytes, result.words.words.map(_.toRect()))
      throw new RuntimeException(s"text($text) can not found")
    }
  }

  @AutoDef
  def extract(): String = {
    val bytes = driver.screenshot(false)
    extract(bytes)
  }

  @AutoDef
  def extract(locator: String): String = {
    extract(locator, Level.SYMBOL, false)
  }

  @AutoDef
  def extract(locator: String, level: String): String = {
    extract(locator, level, false)
  }

  @AutoDef
  def extract(locator: String, level: String, negative: Boolean): String = {
    val bytes = driver.screenshot(locator, false)
    extract(bytes, level, negative)
  }

  @AutoDef
  def extract(bytes: Array[Byte]): String = {
    extract(bytes, Level.SYMBOL, false, true)
  }

  @AutoDef
  def extract(bytes: Array[Byte], level: String): String = {
    extract(bytes, level, false, true)
  }

  @AutoDef
  def extract(bytes: Array[Byte], level: String, negative: Boolean): String = {
    extract(bytes, level, negative, true)
  }

  @AutoDef
  def extract(bytes: Array[Byte], level: String, negative: Boolean, embed: Boolean): String = {
    init()
    val words = tess.process(load(bytes, IMREAD_GRAYSCALE), negative, level)
    if (words.words.nonEmpty && embed) {
      drawAndEmbed(bytes, words.words.map(_.toRect()))
    }
    words.full
  }

  def tesseract(): Tesseract = {
    init()
    tess
  }

  private def parseOcr(raw: String): Pair = {
    val pos = raw.indexOf('}')
    if (pos > -1) {
      val lang = raw.substring(1, pos)
      val text = raw.substring(pos + 1)
      Pair(lang, text)
    } else {
      null
    }
  }

  private def init(): Unit = {
    if (tess == null) {
      val options = driver.getOptions.options
      if (options != null && options.containsKey("tessData") && options.containsKey("tessLang")) tess = Tesseract.apply(options.get("tessData").toString, options.get("tessLang").toString)
      else tess = Tesseract.apply
    }
  }

  override def methodNames(): util.List[String] = Ocr.METHOD_NAMES

}

object Ocr {

  val logger = Logger("OCR")
  val ENGINE_KEY = "ocr"
  val METHOD_NAMES: util.List[String] = Plugin.methodNames(classOf[Ocr])

  case class Pair(left: String, right: String)

}
