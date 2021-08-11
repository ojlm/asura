package asura.ui.jna.platform.mac

import ca.weblite.objc.Client

// https://developer.apple.com/documentation/appkit/nsrunningapplication?language=objc
object NSRunningApplication {

  def active(pid: Long): Unit = {
    val client = Client.getInstance()
    val proxy = client.sendProxy("NSRunningApplication", "runningApplicationWithProcessIdentifier:", pid)
    if (proxy != null) {
      proxy.send("activateWithOptions:", 1 << 1)
      proxy.dispose(false)
    } else {
      throw new RuntimeException(s"Process $pid can not be found")
    }
  }

}
