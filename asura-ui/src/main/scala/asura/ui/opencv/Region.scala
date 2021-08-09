package asura.ui.opencv

import org.bytedeco.opencv.opencv_core.Rect

case class Region(
                   x: Int,
                   y: Int,
                   width: Int,
                   height: Int
                 ) extends asura.ui.model.Region

object Region {
  def apply(rect: Rect): Region = {
    Region(rect.x(), rect.y(), rect.width(), rect.height())
  }
}
