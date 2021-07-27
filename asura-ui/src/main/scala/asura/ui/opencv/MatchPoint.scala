package asura.ui.opencv

case class MatchPoint(
                       scale: Double,
                       minVal: Int,
                       minX: Int,
                       minY: Int,
                       maxVal: Int,
                       maxX: Int,
                       maxY: Int,
                     ) {

  def toRegion(width: Int, height: Int): Region = {
    Region(
      x = Math.round(minX / scale).toInt,
      y = Math.round(minY / scale).toInt,
      width = Math.round(width / scale).toInt,
      height = Math.round(height / scale).toInt,
    )
  }

}
