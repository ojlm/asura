package asura.ui.opencv

import org.bytedeco.opencv.opencv_core.Rect

case class Region(
                   x: Int,
                   y: Int,
                   width: Int,
                   height: Int,
                 ) {

  def toRect(): Rect = new Rect(x, y, width, height)

}
