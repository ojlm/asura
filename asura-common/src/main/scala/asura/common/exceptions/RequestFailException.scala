package asura.common.exceptions

case class RequestFailException(msg: String) extends RuntimeException(msg)
