package asura.ui.jna

import java.awt.{GraphicsEnvironment, Rectangle}

import scala.jdk.CollectionConverters._

import asura.ui.model.Position
import oshi.SystemInfo
import oshi.software.os.OSDesktopWindow

object WindowUtils {

  val os = new SystemInfo().getOperatingSystem()

  def getDesktopWindows(visibleOnly: Boolean = true): Seq[OSDesktopWindow] = {
    os.getDesktopWindows(visibleOnly).asScala.toSeq
  }

  def getDesktopWindow(title: String): Option[OSDesktopWindow] = {
    getDesktopWindowByTitleOrProcessId(title, -1)
  }

  def getDesktopWindow(processId: Long): Option[OSDesktopWindow] = {
    getDesktopWindowByTitleOrProcessId(null, processId)
  }

  def getDesktopWindowByTitleOrProcessId(title: String, pid: Long): Option[OSDesktopWindow] = {
    val value = os.getDesktopWindows(true).stream().filter(window => {
      if (pid != null && pid >= 0) {
        window.getOwningProcessId.equals(pid)
      } else {
        window.getTitle.toLowerCase.contains(title.toLowerCase)
      }
    }).findFirst()
    if (value.isPresent) Some(value.get()) else None
  }

  /** union multiple displays */
  def getAllDisplaysRegion(): Position = {
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment
    var rect = new Rectangle(0, 0, 0, 0)
    env.getScreenDevices.foreach(device => {
      val bounds = device.getDefaultConfiguration.getBounds
      rect = rect.union(bounds)
    })
    Position(rect.x, rect.y, rect.width, rect.height)
  }

}
