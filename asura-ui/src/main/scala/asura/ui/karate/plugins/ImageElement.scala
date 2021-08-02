package asura.ui.karate.plugins

import asura.ui.model.Position
import asura.ui.opencv.OpenCvUtils
import asura.ui.opencv.OpenCvUtils.load
import com.intuit.karate.driver.Driver
import com.intuit.karate.http.ResourceType
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE
import org.bytedeco.opencv.opencv_core.{Mat, Rect}

class ImageElement(
                    val parent: ImageElement,
                    val position: Position,
                    val driver: Driver,
                    val ocr: Ocr, val img: Img, val sys: System,
                  ) extends Element {

  def click(x: Int, y: Int): Element = {
    if (sys != null) {
      sys.click(x, y)
    } else {
      driver.mouse(x, y).click()
    }
    this
  }

  override def click(): Element = {
    click(position.x + position.width / 2, position.y + position.height / 2)
  }

  def getDriverCaptureMat(): Mat = {
    val fullscreen = OpenCvUtils.load(driver.screenshot(false))
    fullscreen.apply(new Rect(position.x, position.y, position.width, position.height))
  }

  def screenshot(embed: Boolean): Array[Byte] = {
    val bytes = if (sys != null) {
      sys.screenshot(position.x, position.y, position.width, position.height, false)
    } else {
      OpenCvUtils.toBytes(getDriverCaptureMat())
    }
    if (embed) {
      driver.getRuntime.embed(bytes, ResourceType.PNG)
    }
    bytes
  }


  override def ocrExtract(level: String, negative: Boolean): String = {
    val bytes = screenshot(false)
    ocr.extract(bytes, level, negative)
  }

  override def ocrClick(text: String, level: String, negative: Boolean): Element = {
    val bytes = screenshot(false)
    val result = ocr.tesseract().find(load(bytes, IMREAD_GRAYSCALE), text, negative, level)
    if (result.found.nonEmpty) {
      val word = result.found(0)
      click(position.x + word.x, position.y + word.y)
      ocr.drawAndEmbed(bytes, result.found.map(_.toRect()))
    } else {
      throw new RuntimeException(s"text($text) can not found")
    }
    this
  }

  override def imgClick(file: String): Element = {
    imgClick(img.loadBytesFromFile(file))
  }

  override def imgClick(image: Array[Byte]): Element = {
    img.click(screenshot(false), image, position)
    this
  }

  override def crop(x: Object): Element = {
    val pos = Position(x, position)
    ImageElement(this, pos, driver, ocr, img, sys)
  }

  override def crop(x: Object, y: Object): Element = {
    val pos = Position(x, y, position)
    ImageElement(this, pos, driver, ocr, img, sys)
  }

  override def crop(x: Object, y: Object, width: Object, height: Object): Element = {
    val pos = Position(x, y, width, height, position)
    ImageElement(this, pos, driver, ocr, img, sys)
  }

  override def getRegion(): Position = position

}

object ImageElement {

  def apply(parent: ImageElement, position: Position, driver: Driver, ocr: Ocr, img: Img): ImageElement = {
    new ImageElement(parent, position, driver, ocr, img, null)
  }

  def apply(parent: ImageElement, position: Position, driver: Driver, ocr: Ocr, img: Img, sys: System): ImageElement = {
    new ImageElement(parent, position, driver, ocr, img, sys)
  }

}
