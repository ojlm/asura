package asura.ui.cli.utils

import asura.common.util.ProcessUtils
import asura.common.util.ProcessUtils.ExecResult

object AdbUtils {

  def startServer(adbPath: String = "adb"): ExecResult = {
    ProcessUtils.exec(s"$adbPath start-server", None)
  }

  def reverse(socketName: String, port: Int, adbPath: String = "adb"): ExecResult = {
    ProcessUtils.exec(s"$adbPath reverse localabstract:$socketName tcp:$port", None)
  }

}
