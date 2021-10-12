package asura.ui.ide

object IdeErrors {

  val WORKSPACE_NAME_ILLEGAL = IdeException("WORKSPACE", "NAME", "ILLEGAL")
  val WORKSPACE_NAME_EXISTS = IdeException("WORKSPACE", "NAME", "EXISTS")
  val WORKSPACE_NAME_EMPTY = IdeException("WORKSPACE", "NAME", "EMPTY")

  val PROJECT_NAME_ILLEGAL = IdeException("PROJECT", "NAME", "ILLEGAL")
  val PROJECT_NAME_EXISTS = IdeException("PROJECT", "NAME", "EXISTS")
  val PROJECT_NAME_EMPTY = IdeException("PROJECT", "NAME", "EMPTY")

  val TREE_NAME_EXISTS = IdeException("TREE", "NAME", "EXISTS")
  val TREE_NAME_EMPTY = IdeException("TREE", "NAME", "EMPTY")
  val TREE_DOC_MISS = IdeException("TREE", "DOC", "MISS")

  val BLOB_TREE_EMPTY = IdeException("BLOB", "TREE", "EMPTY")
  val BLOB_DATA_EMPTY = IdeException("BLOB", "DATA", "EMPTY")
  val BLOB_CREATOR_EMPTY = IdeException("BLOB", "CREATOR", "EMPTY")
  val BLOB_DOC_MISS = IdeException("BLOB", "DOC", "MISS")

}
