package asura.ui.ide.model

import asura.common.util.StringUtils

case class Activity(
                     workspace: String,
                     op: Int,
                     project: String = StringUtils.EMPTY,
                     target: String = StringUtils.EMPTY,
                   ) extends AbsDoc

object Activity {

  // operations
  val OP_INSERT_WORKSPACE = 1

}
