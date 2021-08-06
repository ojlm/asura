package asura.ui.karate.plugins

import java.util

import asura.ui.model.Position
import asura.ui.opencv.OpenCvUtils._
import com.intuit.karate.core.{Plugin, StepResult}
import com.intuit.karate.driver.Driver
import com.intuit.karate.driver.appium.AppiumDriver
import com.intuit.karate.driver.indigo.IndigoDriver
import com.intuit.karate.http.ResourceType
import org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR
import org.bytedeco.opencv.opencv_core.{Mat, Point, Rect}

trait CvPlugin extends Plugin {

  val driver: Driver

  def getRootPosition(): Position = {
    if (driver.isInstanceOf[IndigoDriver] || driver.isInstanceOf[AppiumDriver]) {
      Position(driver.getDimensions)
    } else {
      Position(driver.locate("body").getPosition)
    }
  }

  def embedImage(bytes: Array[Byte]): Unit = {
    getRuntime.embed(bytes, ResourceType.PNG)
  }

  def embedImage(image: Mat): Unit = {
    getRuntime.embed(saveToBytes(image), ResourceType.PNG)
  }

  def drawAndEmbed(image: Mat, text: String): Unit = {
    drawTextOnImage(image, text, Colors.Red)
    getRuntime.embed(saveToBytes(image), ResourceType.PNG)
  }

  def drawAndEmbed(bytes: Array[Byte], rects: Seq[Rect], points: Seq[Point] = Nil): Unit = {
    if (rects.nonEmpty || points.nonEmpty) {
      val image = load(bytes, IMREAD_COLOR)
      rects.foreach(rect => drawRectOnImage(image, rect, Colors.Red))
      points.foreach(point => drawPointOnImage(image, point, 4, Colors.Red, -1))
      getRuntime.embed(saveToBytes(image), ResourceType.PNG)
    }
  }

  override def onFailure(stepResult: StepResult): Unit = {
    if (driver.getOptions.screenshotOnFailure && !stepResult.isWithCallResults) {
      val bytes = driver.screenshot(false)
      getRuntime.embed(bytes, ResourceType.PNG)
    }
  }

  override def afterScenario(): util.Map[String, AnyRef] = java.util.Collections.emptyMap()

}
