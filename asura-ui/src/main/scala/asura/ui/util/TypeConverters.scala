package asura.ui.util

object TypeConverters {

  def toInt(value: Object): Int = {
    if (value.isInstanceOf[Integer]) {
      value.asInstanceOf[Integer]
    } else if (value.isInstanceOf[Double]) {
      value.asInstanceOf[Double].toInt
    } else {
      Integer.parseInt(value.toString)
    }
  }
}
