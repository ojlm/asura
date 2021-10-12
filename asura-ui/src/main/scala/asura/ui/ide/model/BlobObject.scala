package asura.ui.ide.model

import asura.common.util.StringUtils
import asura.ui.ide.IdeErrors

case class BlobObject(
                       var workspace: String,
                       var project: String,
                       var tree: String,
                       var data: Array[Byte],
                       var size: Long,
                     ) extends AbsDoc {

  def parse(isUpdate: Boolean): Unit = {
    if (!isUpdate && StringUtils.isEmpty(tree)) {
      throw IdeErrors.BLOB_TREE_EMPTY
    }
    if (data == null || data.length == 0) {
      throw IdeErrors.BLOB_DATA_EMPTY
    }
    if (StringUtils.isEmpty(creator)) {
      throw IdeErrors.BLOB_CREATOR_EMPTY
    }
    size = data.length
  }

}

object BlobObject {

}
