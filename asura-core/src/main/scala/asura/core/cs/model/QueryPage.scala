package asura.core.cs.model

trait QueryPage {

  val from = 0
  val size = 10

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
