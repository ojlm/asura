package com.intuit.karate.driver.indigo

import java.util.function.Predicate

import asura.common.util.JsonUtils
import asura.ui.message.IndigoMessage
import com.intuit.karate.Logger
import com.intuit.karate.driver.DriverOptions

case class IndigoWait(options: DriverOptions, driver: IndigoDriver) {

  private var lastSent: IndigoMessage = null
  private var condition: Predicate[IndigoMessage] = null
  private var lastReceived: IndigoMessage = null

  private val DEFAULT: Predicate[IndigoMessage] = m => lastSent.id == m.id
  private val logger: Logger = IndigoDriver.logger

  def send(req: IndigoMessage, cond: Predicate[IndigoMessage] = DEFAULT): IndigoMessage = {
    lastReceived = null
    lastSent = req
    condition = if (cond != null) cond else DEFAULT
    this.synchronized {
      logger.trace(s">> wait: ${JsonUtils.stringify(req)}")
      try {
        driver.send(req)
        wait(options.timeout)
      } catch {
        case t: InterruptedException =>
          logger.error(s"interrupted: ${t.getMessage} wait: ${JsonUtils.stringify(req)}");
      }
    }
    if (lastReceived != null) {
      logger.trace(s"<< notified: ${JsonUtils.stringify(req)}")
      lastReceived
    } else {
      logger.trace(s"<< timed out after milliseconds: ${options.timeout} - ${JsonUtils.stringify(req)}")
      null
    }
  }

  def receive(res: IndigoMessage): Unit = {
    if (condition != null) {
      this.synchronized {
        if (condition.test(res)) {
          logger.trace(s"<< notify: ${JsonUtils.stringify(res)}")
          lastReceived = res
          notify()
        } else {
          logger.trace(s"<< ignore: ${JsonUtils.stringify(res)}")
        }
      }
    }
  }

}
