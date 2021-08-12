package com.intuit.karate.driver.indigo

import asura.ui.message.builder.TouchEventParams
import com.intuit.karate.driver.Mouse

class IndigoMouse(driver: IndigoDriver) extends Mouse {

  var x: Double = 0
  var y: Double = 0

  @throws[RuntimeException]
  def alert[T](): T = {
    throw new RuntimeException("Not support")
  }

  override def move(locator: String): IndigoMouse = {
    val pos = driver.rect(locator)
    x = pos.x + pos.width / 2
    y = pos.y + pos.height / 2
    this
  }

  override def move(x: Number, y: Number): IndigoMouse = {
    if (x != null) this.x = x.doubleValue()
    if (y != null) this.y = y.doubleValue()
    this
  }

  override def offset(x: Number, y: Number): IndigoMouse = {
    if (x != null) this.x = this.x + x.doubleValue()
    if (y != null) this.y = this.y + y.doubleValue()
    this
  }

  override def down(): IndigoMouse = {
    driver.touchDown(TouchEventParams(x, y))
    this
  }

  override def up(): IndigoMouse = {
    driver.touchUp(TouchEventParams(x, y))
    this
  }

  override def submit(): IndigoMouse = alert()

  override def click(): IndigoMouse = {
    driver.touchDown(TouchEventParams(x, y))
    this
  }

  override def doubleClick(): IndigoMouse = alert()

  override def go(): IndigoMouse = {
    this
  }

  override def duration(duration: Integer): IndigoMouse = alert()

}

object IndigoMouse {

  def apply(driver: IndigoDriver): IndigoMouse = new IndigoMouse(driver)

  def apply(driver: IndigoDriver, locator: String): IndigoMouse = {
    new IndigoMouse(driver).move(locator)
  }

  def apply(driver: IndigoDriver, x: Int, y: Int): IndigoMouse = {
    new IndigoMouse(driver).move(x, y)
  }

}
