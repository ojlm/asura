package asura.ui.karate.plugins

import java.util
import java.util.Collections

import asura.ui.model.Position
import asura.ui.ocr.Tesseract.Level
import asura.ui.opencv.detector.Detector

trait Element {

  def click(): Element

  def screenshot(): Array[Byte] = screenshot(true)

  def screenshot(embed: Boolean): Array[Byte]

  def ocrExtract(): String = ocrExtract(Level.WORD, false)

  def ocrExtract(level: String): String = ocrExtract(level, false)

  def ocrExtract(level: String, negative: Boolean): String

  def ocrClick(word: String): Element = ocrClick(word, Level.WORD, false)

  def ocrClick(word: String, level: String): Element = ocrClick(word, level, false)

  def ocrClick(word: String, level: String, negative: Boolean): Element

  def imgClick(file: String): Element

  def imgClick(image: Array[Byte]): Element

  def compare(file: String): Double

  def compare(image: Array[Byte]): Double

  def detect(): Element = detect(Detector.DEFAULT, Collections.emptyMap[String, Any]())

  def detect(method: String): Element = detect(method, Collections.emptyMap[String, Any]())

  def detect(method: String, options: util.Map[String, Any]): Element

  def crop(x: Object): Element

  def crop(x: Object, y: Object): Element

  def crop(x: Object, y: Object, width: Object, height: Object): Element

  def highlight(): Element = highlight(2000)

  def highlight(time: Int): Element

  /**
   * @return current absolute position
   */
  def getRegion(): Position

  def getPosition(): java.util.Map[String, Integer] = {
    getRegion().toMap()
  }

}
