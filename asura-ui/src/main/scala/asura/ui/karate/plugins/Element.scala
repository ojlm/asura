package asura.ui.karate.plugins

import asura.ui.model.Position
import asura.ui.ocr.Tesseract.Level

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

  def crop(x: Object): Element

  def crop(x: Object, y: Object): Element

  def crop(x: Object, y: Object, width: Object, height: Object): Element

  def getRegion(): Position

  def getPosition(): java.util.Map[String, Integer] = {
    getRegion().toMap()
  }

}
