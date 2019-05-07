package asura.core.util

import asura.common.util.StringUtils

object CommonValidator {

  def isIdLegal(id: String): Boolean = {
    if (StringUtils.isEmpty(id)) {
      false
    } else {
      id.forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c.equals('_') || c.equals('-') || c.equals('.'))
    }
  }
}
