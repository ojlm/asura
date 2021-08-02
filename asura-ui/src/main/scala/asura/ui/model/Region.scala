package asura.ui.model

import org.bytedeco.opencv.opencv_core.Rect

trait Region {

  def x: Int

  def y: Int

  def width: Int

  def height: Int

  def toRect(): Rect = new Rect(x, y, width, height)

  def toPosition(): Position = Position(x, y, width, height)

  def toMap(): java.util.Map[String, Integer] = {
    val map = new java.util.HashMap[String, Integer]()
    map.put("x", x)
    map.put("y", y)
    map.put("width", width)
    map.put("height", height)
    map
  }

}
