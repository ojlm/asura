package asura.ui.model

case class IntPoint(var x: Int, var y: Int) {

  def offset(offsetX: Int, offsetY: Int): Unit = {
    x = x + offsetX
    y = y + offsetY
  }

}
