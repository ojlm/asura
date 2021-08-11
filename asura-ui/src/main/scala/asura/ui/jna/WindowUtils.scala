package asura.ui.jna

import java.awt.{GraphicsEnvironment, Rectangle}
import java.util.stream.Collectors

import asura.ui.jna.platform.mac.NSRunningApplication
import asura.ui.model.Position
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.{Platform, Pointer}
import oshi.SystemInfo
import oshi.software.os.OSDesktopWindow

object WindowUtils {

  val os = new SystemInfo().getOperatingSystem()

  def getDesktopWindows(visibleOnly: Boolean = true): java.util.List[OSDesktopWindow] = {
    os.getDesktopWindows(visibleOnly)
  }

  def getDesktopWindows(title: String): java.util.List[OSDesktopWindow] = {
    getDesktopWindowByTitleOrProcessId(title, -1)
  }

  def getDesktopWindow(processId: Long): java.util.List[OSDesktopWindow] = {
    getDesktopWindowByTitleOrProcessId(null, processId)
  }

  def getDesktopWindowById(id: Long): Option[OSDesktopWindow] = {
    val value = os.getDesktopWindows(true).stream().filter(window => {
      window.getWindowId == id
    }).findFirst()
    if (value.isPresent) Some(value.get()) else None
  }

  def getDesktopWindowByTitleOrProcessId(title: String, pid: Long): java.util.List[OSDesktopWindow] = {
    os.getDesktopWindows(true).stream()
      .filter(window => {
        if (pid != null && pid >= 0) {
          window.getOwningProcessId.equals(pid)
        } else {
          window.getTitle.toLowerCase.contains(title.toLowerCase)
        }
      })
      .sorted((o1, o2) => {
        if (o1.getOrder != o2.getOrder) {
          o1.getOrder - o2.getOrder
        } else {
          (o1.getWindowId - o2.getWindowId).toInt
        }
      })
      .collect(Collectors.toList[OSDesktopWindow])
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

  def active(window: OSDesktopWindow): Unit = {
    if (Platform.isWindows()) {
      User32.INSTANCE.SetForegroundWindow(new HWND(new Pointer(window.getWindowId)))
    } else if (Platform.isMac()) {
      NSRunningApplication.active(window.getOwningProcessId)
    } else {
      throw new RuntimeException(s"Not available on this platform: ${Platform.getOSType}")
    }
  }

}
