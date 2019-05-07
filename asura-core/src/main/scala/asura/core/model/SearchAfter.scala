package asura.core.model

trait SearchAfter {

  var group: String = null
  var project: String = null
  var creator: String = null
  var text: String = null
  var size = 10
  var sort: Seq[Any] = Nil

  def pageSize = {
    if (Option(size).isDefined && size > 0) {
      size
    } else {
      10
    }
  }

  def toSearchAfterSort = if (null != sort) sort else Nil
}
