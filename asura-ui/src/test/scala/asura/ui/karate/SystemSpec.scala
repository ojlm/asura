package asura.ui.karate

import asura.ui.karate.plugins.System
import asura.ui.model.Position
import asura.ui.opencv.OpenCvUtils

object SystemSpec {

  def main(args: Array[String]): Unit = {
    val sys = new System(null, null, null, true)
    println(s"screen: ${sys.toolkit.getScreenSize}")
    sys.move(400, 500)
    System.highlightAll(Position(100, 100, 600, 600), Seq(
      Position(110, 110, 100, 80, "aa"),
      Position(200, 110, 100, 60, "bb"),
    ), 3000, true)
    val bytes = sys.screenshot(false)
    OpenCvUtils.show(OpenCvUtils.load(bytes), "screenshot")
  }

}
