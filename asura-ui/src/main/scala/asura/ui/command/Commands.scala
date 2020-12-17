package asura.ui.command

object Commands {

  val MONKEY = "monkey"
  val KARATE = "karate"

  def support(command: String): Boolean = {
    command match {
      case MONKEY | KARATE => true
      case _ => false
    }
  }

}
