package asura.app.api.model

import asura.app.api.model.Dtabs.DtabItem
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.ErrorMessages.ErrorMessage

case class Dtabs(dtabs: Seq[DtabItem])

object Dtabs {

  case class DtabItem(group: String, project: String, namespace: String, host: String, port: String, owned: Boolean) {

    def isValid(): ErrorMessage = {
      if (StringUtils.isEmpty(group)) {
        ErrorMessages.error_EmptyGroup
      } else if (StringUtils.isEmpty(project)) {
        ErrorMessages.error_EmptyProject
      } else if (StringUtils.isEmpty(namespace)) {
        ErrorMessages.error_EmptyNamespace
      } else if (namespace.contains("/")) {
        ErrorMessages.error_IllegalCharacter("/")
      } else {
        null
      }
    }
  }

}
