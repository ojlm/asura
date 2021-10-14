package asura.ui.ide.model

import asura.common.util.StringUtils
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.TreeObject.{TYPE_DIRECTORY, TYPE_DIRECTORY_LINK}

case class TreeObject(
                       var workspace: String,
                       var project: String,
                       var name: String,
                       var blob: String = StringUtils.EMPTY,
                       var extension: String = StringUtils.EMPTY,
                       var parent: String = StringUtils.EMPTY,
                       var size: Long = 0L,
                       var `type`: Int = TreeObject.TYPE_FILE,
                     ) extends AbsDoc {

  def parse(isUpdate: Boolean): Unit = {
    if (!isUpdate && StringUtils.isEmpty(workspace)) {
      throw IdeErrors.WORKSPACE_NAME_EMPTY
    } else if (!isUpdate && StringUtils.isEmpty(project)) {
      throw IdeErrors.PROJECT_NAME_EMPTY
    } else if (StringUtils.isEmpty(name)) {
      throw IdeErrors.TREE_NAME_EMPTY
    } else {
      if (`type` != TYPE_DIRECTORY && `type` != TYPE_DIRECTORY_LINK && StringUtils.isNotEmpty(name)) {
        val idx = name.lastIndexOf('.')
        if (idx > -1) {
          extension = name.substring(idx + 1)
        }
      }
      if (blob == null) blob = StringUtils.EMPTY
      if (extension == null) extension = StringUtils.EMPTY
      if (parent == null) parent = StringUtils.EMPTY
    }
  }

}

object TreeObject {

  val TYPE_NONE = -1
  val TYPE_DIRECTORY = 0
  val TYPE_DIRECTORY_LINK = 1
  val TYPE_FILE = 2
  val TYPE_FILE_LINK = 3

}
