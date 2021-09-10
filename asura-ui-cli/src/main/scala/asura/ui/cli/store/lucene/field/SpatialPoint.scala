package asura.ui.cli.store.lucene.field

case class SpatialPoint(latitude: Double, longitude: Double) {
  override def toString: String = s"$latitude,$longitude"
}

object SpatialPoint {
  def apply(s: String): SpatialPoint = {
    val index = s.indexOf(',')
    val latitude = s.substring(0, index).toDouble
    val longitude = s.substring(index + 1).toDouble
    SpatialPoint(latitude, longitude)
  }
}
