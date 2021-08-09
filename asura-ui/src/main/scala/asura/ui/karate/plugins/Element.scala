package asura.ui.karate.plugins

import java.util
import java.util.Collections

import asura.ui.model.Position
import asura.ui.ocr.FindResult
import asura.ui.ocr.Tesseract.Level
import asura.ui.opencv.detector.Detector

trait Element {

  def click(): Element

  def screenshot(): Array[Byte] = screenshot(true)

  def screenshot(embed: Boolean): Array[Byte]

  def ocrExtract(): String = ocrExtract(Level.SYMBOL, false)

  def ocrExtract(level: String): String = ocrExtract(level, false, true)

  def ocrExtract(level: String, negative: Boolean): String = ocrExtract(level, negative, true)

  def ocrExtract(level: String, negative: Boolean, embed: Boolean): String

  def ocrClick(word: String): Element = ocrClick(word, Level.SYMBOL, false)

  def ocrClick(word: String, level: String): Element = ocrClick(word, level, false, true)

  def ocrClick(word: String, level: String, negative: Boolean): Element = ocrClick(word, level, negative, true)

  def ocrClick(word: String, level: String, negative: Boolean, embed: Boolean): Element

  def ocrFind(text: String, level: String, negative: Boolean): FindResult

  def imgClick(file: String): Element

  def imgClick(image: Array[Byte]): Element

  def compare(file: String): Double

  def compare(image: Array[Byte]): Double

  def detect(): Element = detect(Detector.DEFAULT, Collections.emptyMap[String, Any]())

  def detect(method: String): Element = detect(method, Collections.emptyMap[String, Any]())

  def detect(method: String, options: util.Map[String, Any]): Element

  def `match`(file: String): util.List[Element]

  def `match`(image: Array[Byte]): util.List[Element]

  def find(locator: Any): Element = find(locator, 4, 4)

  // x,y with padding
  def find(locator: Any, x: Int, y: Int): Element

  def findAll(locator: String): util.List[Element] = findAll(locator, 4, 4)

  def findAll(locator: String, x: Int, y: Int): util.List[Element]

  def regions(): util.List[util.Map[String, Integer]]

  def crop(x: Any): Element

  def crop(x: Any, y: Any): Element

  def crop(x: Any, y: Any, width: Any, height: Any): Element

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
