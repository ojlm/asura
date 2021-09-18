package asura.ui.ide

object IdeErrors {

  val WORKSPACE_NAME_ILLEGAL = IdeException("WORKSPACE", "NAME", "ILLEGAL")
  val WORKSPACE_NAME_EXISTS = IdeException("WORKSPACE", "NAME", "EXISTS")
  val WORKSPACE_NAME_EMPTY = IdeException("WORKSPACE", "NAME", "EMPTY")

  val PROJECT_NAME_ILLEGAL = IdeException("PROJECT", "NAME", "ILLEGAL")
  val PROJECT_NAME_EXISTS = IdeException("PROJECT", "NAME", "EXISTS")
  val PROJECT_NAME_EMPTY = IdeException("PROJECT", "NAME", "EMPTY")

}
