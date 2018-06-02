package asura.common.util

object XtermUtils {

  def redWrap(msg: String): String = {
    s"\033[1;31m$msg\033[0m"
  }

  def greenWrap(msg: String): String = {
    s"\033[1;32m$msg\033[0m"
  }

  def yellowWrap(msg: String): String = {
    s"\033[1;33m$msg\033[0m"
  }

  def blueWrap(msg: String): String = {
    s"\033[1;34m$msg\033[0m"
  }
}
