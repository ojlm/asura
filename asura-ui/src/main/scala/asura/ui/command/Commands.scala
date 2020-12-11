package asura.ui.command

object Commands {

  val MONKEY = "monkey"

  def support(command: String): Boolean = {
    command match {
      case MONKEY => true
      case _ => false
    }
  }

}
