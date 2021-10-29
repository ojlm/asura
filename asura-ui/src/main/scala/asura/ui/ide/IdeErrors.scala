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
  val TREE_PATH_EMPTY = IdeException("TREE", "PATH", "EMPTY")
  val TREE_DIRECTORY_DELETE = IdeException("TREE", "DIRECTORY", "DELETE")

  val BLOB_TREE_EMPTY = IdeException("BLOB", "TREE", "EMPTY")
  val BLOB_DATA_EMPTY = IdeException("BLOB", "DATA", "EMPTY")
  val BLOB_CREATOR_EMPTY = IdeException("BLOB", "CREATOR", "EMPTY")
  val BLOB_DOC_MISS = IdeException("BLOB", "DOC", "MISS")

  val TASK_TYPE_ILLEGAL = IdeException("TASK", "TYPE", "ILLEGAL")
  val TASK_DRIVER_ILLEGAL = IdeException("TASK", "DRIVER", "ILLEGAL")

  def TREE_DOC_MISS_MSG(msg: String) = IdeException("TREE", "DOC", "MISS", msg)

  def TREE_NAME_EXISTS_MSG(msg: String) = IdeException("TREE", "NAME", "EXISTS", msg)

  def TASK_DRIVER_ILLEGAL_MSG(msg: String) = IdeException("TASK", "DRIVER", "ILLEGAL", msg)

  def RECORD_SOURCE_ILLEGAL_MSG(msg: String) = IdeException("RECORD", "SOURCE", "ILLEGAL", msg)

}
