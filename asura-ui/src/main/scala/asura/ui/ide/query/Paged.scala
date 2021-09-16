package asura.ui.ide.query

trait Paged {

  var from = 0
  var size = 10

  def offset: Int = {
    if (Option(from).isDefined && from >= 0) {
      from
    } else {
      0
    }
  }

  def limit: Int = {
    if (Option(size).isDefined && size > 0) {
      size
    } else {
      10
    }
  }

}
