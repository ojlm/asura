package asura.ui.ide

import asura.common.util.StringUtils

case class IdeException(
                         code: String,
                         msg: String,
                       ) extends RuntimeException(StringUtils.notEmptyElse(msg, code))

object IdeException {
  def apply(main: String, sub: String, act: String, msg: String = null): IdeException = {
    IdeException(s"${main.toLowerCase}.${sub.toLowerCase}.${act.toLowerCase}", msg)
  }
}
