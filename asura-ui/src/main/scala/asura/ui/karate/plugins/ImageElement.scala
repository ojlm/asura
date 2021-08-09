package asura.ui.karate.plugins

import java.util

import asura.ui.model.Position
import asura.ui.ocr.FindResult
import asura.ui.ocr.Tesseract.Level
import asura.ui.opencv.OpenCvUtils.load
import asura.ui.opencv.detector.DetectorResult
import asura.ui.opencv.{MatchResult, OpenCvUtils}
import com.intuit.karate.driver.Driver
import com.intuit.karate.http.ResourceType
import com.typesafe.scalalogging.Logger
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE
import org.bytedeco.opencv.opencv_core.{Mat, Rect}

class ImageElement(
                    val parent: ImageElement,
                    val position: Position,
                    val driver: Driver,
                    val ocr: Ocr, val img: Img, val sys: System,
                  ) extends Element {

  var result: DetectorResult = null

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

  override def ocrExtract(level: String, negative: Boolean, embed: Boolean): String = {
    val bytes = screenshot(false)
    ocr.extract(bytes, level, negative, embed)
  }

  override def ocrFind(text: String, level: String, negative: Boolean): FindResult = {
    ocr.tesseract().find(load(screenshot(false), IMREAD_GRAYSCALE), text, negative, level)
  }

  override def ocrClick(text: String, level: String, negative: Boolean, embed: Boolean): Element = {
    val bytes = screenshot(false)
    val result = ocrFind(text, level, negative)
    if (result.found.nonEmpty) {
      val word = result.found(0)
      click(position.x + word.x, position.y + word.y)
      if (embed) ocr.drawAndEmbed(bytes, result.found.map(_.toRect()))
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

  override def compare(file: String): Double = {
    img.compare(screenshot(false), file)
  }

  override def compare(image: Array[Byte]): Double = {
    img.compare(screenshot(false), image)
  }

  override def detect(method: String, options: util.Map[String, Any]): Element = {
    result = img.detect(screenshot(false), method, options)
    this
  }

  def matchResultToElements(result: MatchResult): util.List[Element] = {
    val list = new util.ArrayList[Element]()
    result.regions.foreach(region => {
      list.add(crop(region, region.y, region.width, region.height))
    })
    list
  }

  override def `match`(file: String): util.List[Element] = {
    matchResultToElements(img.`match`(screenshot(false), file))
  }

  override def `match`(image: Array[Byte]): util.List[Element] = {
    matchResultToElements(img.`match`(screenshot(false), image))
  }

  override def find(locator: Any, x: Int, y: Int): Element = {
    locator match {
      case text: String =>
        val found = findAll(text, x, y)
        if (found.size() > 0) {
          found.get(0)
        } else {
          throw new RuntimeException(s"Can't find: $locator")
        }
      case idx: Int =>
        if (result == null) detect()
        val regions = result.regions()
        if (idx > -1 && idx < regions.length) {
          val region = regions(idx)
          crop(region.x - x, region.y - y, region.width + x, region.y + y)
        } else {
          throw new RuntimeException(s"Index out of bounds(${regions.length}): $idx")
        }
      case _ =>
        throw new RuntimeException(s"Unknown expression: $locator")
    }
  }

  override def findAll(locator: String, x: Int, y: Int): util.List[Element] = {
    if (result == null) detect()
    val found = new util.ArrayList[Element]()
    result.regions().foreach(region => {
      try {
        val posX = region.x - x
        val posY = region.y - y
        val ele = crop(if (posX > 0) posX else region.x, if (posY > 0) posY else region.y, region.width + x, region.height + y)
        val result = ele.ocrFind(locator, Level.SYMBOL, false)
        if (result.found.nonEmpty) {
          found.add(ele)
        }
      } catch {
        case t: Throwable => ImageElement.logger.info(s"find $locator error: ${t.getMessage}")
      }
    })
    found
  }

  override def regions(): util.List[util.Map[String, Integer]] = {
    if (result == null) detect()
    val list = new util.ArrayList[util.Map[String, Integer]]()
    result.regions().foreach(region => list.add(region.toMap()))
    list
  }

  override def crop(x: Any): Element = {
    val pos = Position(x, position)
    ImageElement(this, pos, driver, ocr, img, sys)
  }

  override def crop(x: Any, y: Any): Element = {
    val pos = Position(x, y, position)
    ImageElement(this, pos, driver, ocr, img, sys)
  }

  override def crop(x: Any, y: Any, width: Any, height: Any): Element = {
    val pos = Position(x, y, width, height, position)
    ImageElement(this, pos, driver, ocr, img, sys)
  }

  override def highlight(time: Int): Element = {
    if (sys != null) {
      sys.highlight(position, time)
    } else {
      // todo
    }
    this
  }

  override def getRegion(): Position = position

  override def toString: String = {
    s"{x: ${position.x}, y: ${position.y}, width: ${position.width}, height: ${position.height}}"
  }

}

object ImageElement {

  val logger = Logger("ImageElement")

  def apply(parent: ImageElement, position: Position, driver: Driver, ocr: Ocr, img: Img): ImageElement = {
    new ImageElement(parent, position, driver, ocr, img, null)
  }

  def apply(parent: ImageElement, position: Position, driver: Driver, ocr: Ocr, img: Img, sys: System): ImageElement = {
    new ImageElement(parent, position, driver, ocr, img, sys)
  }

}
