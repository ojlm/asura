package asura.core.model

trait QueryPage {

  var from = 0
  var size = 10

  def pageFrom = {
    if (Option(from).isDefined && from >= 0) {
      from
    } else {
      0
    }
  }

  def pageSize = {
    if (Option(size).isDefined && size > 0) {
      size
    } else {
      10
    }
  }
}
