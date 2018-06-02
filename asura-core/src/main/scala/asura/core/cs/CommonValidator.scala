package asura.core.cs

import asura.common.util.StringUtils

object CommonValidator {

  def isIdLegal(id: String): Boolean = {
    if (StringUtils.isEmpty(id)) {
      false
    } else {
      id.forall(c => Character.isLetterOrDigit(c) || c.equals('_') || c.equals('-') || c.equals('.'))
    }
  }
}
