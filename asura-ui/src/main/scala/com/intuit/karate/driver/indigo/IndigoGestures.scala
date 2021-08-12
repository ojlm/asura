package com.intuit.karate.driver.indigo

import asura.ui.message.builder._
import com.intuit.karate.core.AutoDef

class IndigoGestures(driver: IndigoDriver) {

  implicit val SendFunction = driver.SendFunction

  @AutoDef
  def drag(locator: String, x: Double, y: Double): Unit = {
    val body = GesturesDrag.DragModel(ElementModel(driver.elementId(locator)), null, PointModel(x, y))
    GesturesDrag(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def drag(locator: String, x: Double, y: Double, speed: Int): Unit = {
    val body = GesturesDrag.DragModel(ElementModel(driver.elementId(locator)), null, PointModel(x, y), speed)
    GesturesDrag(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def drag(startX: Double, startY: Double, endX: Double, endY: Double): Unit = {
    val body = GesturesDrag.DragModel(null, PointModel(startX, startY), PointModel(endX, endY))
    GesturesDrag(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def drag(startX: Double, startY: Double, endX: Double, endY: Double, speed: Int): Unit = {
    val body = GesturesDrag.DragModel(null, PointModel(startX, startY), PointModel(endX, endY), speed)
    GesturesDrag(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def fling(locator: String, direction: String): Unit = {
    val body = GesturesFling.FlingModel(ElementModel(driver.elementId(locator)), null, direction)
    GesturesFling(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def fling(locator: String, direction: String, speed: Int): Unit = {
    val body = GesturesFling.FlingModel(ElementModel(driver.elementId(locator)), null, direction, speed)
    GesturesFling(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def fling(top: Double, left: Double, width: Double, height: Double, direction: String): Unit = {
    val body = GesturesFling.FlingModel(null, RectModel(top, left, width, height), direction)
    GesturesFling(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def fling(top: Double, left: Double, width: Double, height: Double, direction: String, speed: Int): Unit = {
    val body = GesturesFling.FlingModel(null, RectModel(top, left, width, height), direction, speed)
    GesturesFling(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def longClick(locator: String): Unit = {
    val body = GesturesLongClick.LongClickModel(ElementModel(driver.elementId(locator)), null)
    GesturesLongClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def longClick(locator: String, duration: Double): Unit = {
    val body = GesturesLongClick.LongClickModel(ElementModel(driver.elementId(locator)), null, duration)
    GesturesLongClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def longClick(x: Double, y: Double): Unit = {
    val body = GesturesLongClick.LongClickModel(null, PointModel(x, y))
    GesturesLongClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def longClick(x: Double, y: Double, duration: Double): Unit = {
    val body = GesturesLongClick.LongClickModel(null, PointModel(x, y), duration)
    GesturesLongClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def longClick(locator: String, x: Double, y: Double, duration: Double): Unit = {
    val body = GesturesLongClick.LongClickModel(ElementModel(driver.elementId(locator)), PointModel(x, y), duration)
    GesturesLongClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def doubleClick(locator: String): Unit = {
    val body = GesturesDoubleClick.DoubleClickModel(ElementModel(driver.elementId(locator)), null)
    GesturesDoubleClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def doubleClick(x: Double, y: Double): Unit = {
    val body = GesturesDoubleClick.DoubleClickModel(null, PointModel(x, y))
    GesturesDoubleClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def doubleClick(locator: String, x: Double, y: Double): Unit = {
    val body = GesturesDoubleClick.DoubleClickModel(ElementModel(driver.elementId(locator)), PointModel(x, y))
    GesturesDoubleClick(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def pinchClose(locator: String, percent: Float): Unit = {
    val body = PinchModel(ElementModel(driver.elementId(locator)), null, percent)
    GesturesPinchClose(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def gesturesPinchClose(locator: String, percent: Float, speed: Int): Unit = {
    val body = PinchModel(ElementModel(driver.elementId(locator)), null, percent, speed)
    GesturesPinchClose(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def pinchClose(top: Double, left: Double, width: Double, height: Double, percent: Float): Unit = {
    val body = PinchModel(null, RectModel(top, left, width, height), percent)
    GesturesPinchClose(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def pinchClose(top: Double, left: Double, width: Double, height: Double, percent: Float, speed: Int): Unit = {
    val body = PinchModel(null, RectModel(top, left, width, height), percent, speed)
    GesturesPinchClose(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def pinchOpen(locator: String, percent: Float): Unit = {
    val body = PinchModel(ElementModel(driver.elementId(locator)), null, percent)
    GesturesPinchOpen(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def pinchOpen(locator: String, percent: Float, speed: Int): Unit = {
    val body = PinchModel(ElementModel(driver.elementId(locator)), null, percent, speed)
    GesturesPinchOpen(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def pinchOpen(top: Double, left: Double, width: Double, height: Double, percent: Float): Unit = {
    val body = PinchModel(null, RectModel(top, left, width, height), percent)
    GesturesPinchClose(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def pinchOpen(top: Double, left: Double, width: Double, height: Double, percent: Float, speed: Int): Unit = {
    val body = PinchModel(null, RectModel(top, left, width, height), percent, speed)
    GesturesPinchClose(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def scroll(locator: String, direction: String, percent: Float): Boolean = {
    val body = GesturesScroll.ScrollModel(ElementModel(driver.elementId(locator)), null, direction, percent)
    GesturesScroll(driver.sessionId, body).withId(driver.nextId())
      .send().resBodyAs(classOf[GesturesScroll.Response]).value
  }

  @AutoDef
  def scroll(locator: String, direction: String, percent: Float, speed: Int): Boolean = {
    val body = GesturesScroll.ScrollModel(ElementModel(driver.elementId(locator)), null, direction, percent, speed)
    GesturesScroll(driver.sessionId, body).withId(driver.nextId())
      .send().resBodyAs(classOf[GesturesScroll.Response]).value
  }

  @AutoDef
  def scroll(top: Double, left: Double, width: Double, height: Double, direction: String, percent: Float): Boolean = {
    val body = GesturesScroll.ScrollModel(null, RectModel(top, left, width, height), direction, percent)
    GesturesScroll(driver.sessionId, body).withId(driver.nextId())
      .send().resBodyAs(classOf[GesturesScroll.Response]).value
  }

  @AutoDef
  def scroll(top: Double, left: Double, width: Double, height: Double, direction: String, percent: Float, speed: Int): Boolean = {
    val body = GesturesScroll.ScrollModel(null, RectModel(top, left, width, height), direction, percent, speed)
    GesturesScroll(driver.sessionId, body).withId(driver.nextId())
      .send().resBodyAs(classOf[GesturesScroll.Response]).value
  }

  @AutoDef
  def swipe(locator: String, direction: String, percent: Float): Unit = {
    val body = GesturesSwipe.SwipeModel(ElementModel(driver.elementId(locator)), null, direction, percent)
    GesturesSwipe(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def swipe(locator: String, direction: String, percent: Float, speed: Int): Unit = {
    val body = GesturesSwipe.SwipeModel(ElementModel(driver.elementId(locator)), null, direction, percent, speed)
    GesturesSwipe(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def swipe(top: Double, left: Double, width: Double, height: Double, direction: String, percent: Float): Unit = {
    val body = GesturesSwipe.SwipeModel(null, RectModel(top, left, width, height), direction, percent)
    GesturesSwipe(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

  @AutoDef
  def swipe(top: Double, left: Double, width: Double, height: Double, direction: String, percent: Float, speed: Int): Unit = {
    val body = GesturesSwipe.SwipeModel(null, RectModel(top, left, width, height), direction, percent, speed)
    GesturesSwipe(driver.sessionId, body).withId(driver.nextId()).send().ok()
  }

}


