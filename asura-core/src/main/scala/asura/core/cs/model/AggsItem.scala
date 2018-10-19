package asura.core.cs.model

import asura.common.util.StringUtils

case class AggsItem(
                     val `type`: String,
                     val id: String,
                     val count: Int,
                     var summary: String = StringUtils.EMPTY,
                     var description: String = StringUtils.EMPTY,
                   ) {

}
