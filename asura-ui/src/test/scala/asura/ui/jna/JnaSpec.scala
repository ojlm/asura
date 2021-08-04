package asura.ui.jna

import java.awt.{GraphicsEnvironment, Rectangle}

object JnaSpec {

  def main(args: Array[String]): Unit = {
    WindowUtils.getDesktopWindow("Chrome").map(window => {
      println(s"window: $window")
    })
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment
    var rect = new Rectangle(0, 0, 0, 0)
    env.getScreenDevices.foreach(device => {
      val mode = device.getDisplayMode
      println(s"mode: $mode")
      val bounds = device.getDefaultConfiguration.getBounds
      println(s"bounds: $bounds")
      rect = rect.union(bounds)
    })
    println(s"rect $rect")
  }

}
