package asura.ui.command

object Commands {

  val WEB_MONKEY = "web.monkey"
  val KARATE = "karate"

  def support(command: String): Boolean = {
    command match {
      case WEB_MONKEY | KARATE => true
      case _ => false
    }
  }

}
