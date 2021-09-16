package asura.ui.ide.model

import asura.common.util.StringUtils
import asura.ui.ide.model.UserPreference.LatestPreference

case class UserPreference(
                           var username: String,
                           var alias: String = StringUtils.EMPTY,
                           var email: String = StringUtils.EMPTY,
                           var avatar: String = StringUtils.EMPTY,
                           var description: String = StringUtils.EMPTY,
                           var latest: LatestPreference = null,
                         ) extends AbsDoc

object UserPreference {

  case class LatestPreference(workspace: String)

}
