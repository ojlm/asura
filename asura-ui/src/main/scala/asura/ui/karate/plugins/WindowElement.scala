package asura.ui.karate.plugins

import java.awt.Rectangle

import asura.ui.jna.WindowUtils

case class WindowElement(title: String, pid: Long) {

  def getWindowRect(): Rectangle = {
    val window = WindowUtils.getDesktopWindowByTitleOrProcessId(title, pid)
    if (window.nonEmpty) {
      window.get.getLocAndSize
    } else {
      throw new RuntimeException(s"window ${if (title != null) title else pid} is disappeared")
    }
  }

}
