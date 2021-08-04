package asura.ui.karate.plugins

import java.awt.Rectangle

import asura.ui.jna.WindowUtils

case class WindowElement(id: Long, title: String, pid: Long) {

  def getWindowRect(): Rectangle = {
    val window = WindowUtils.getDesktopWindowById(id)
    if (window.nonEmpty) {
      window.get.getLocAndSize
    } else {
      throw new RuntimeException(s"window ${if (title != null) title else pid} is disappeared")
    }
  }

}
