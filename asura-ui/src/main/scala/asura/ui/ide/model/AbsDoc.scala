package asura.ui.ide.model

import asura.common.util.{DateUtils, StringUtils}

trait AbsDoc {
  var id: String = StringUtils.EMPTY
  var creator: String = StringUtils.EMPTY
  var createdAt: java.lang.Long = DateUtils.now()
  var updatedAt: java.lang.Long = DateUtils.now()
}
