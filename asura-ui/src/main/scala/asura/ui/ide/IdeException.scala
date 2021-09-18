package asura.ui.ide

case class IdeException(name: String) extends RuntimeException(name)

object IdeException {
  def apply(main: String, sub: String, msg: String): IdeException = {
    IdeException(s"${main.toLowerCase}.${sub.toLowerCase}.${msg.toLowerCase}")
  }
}
