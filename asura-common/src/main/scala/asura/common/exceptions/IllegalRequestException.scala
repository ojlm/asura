package asura.common.exceptions

case class IllegalRequestException(msg: String, data: Any = null) extends RuntimeException(msg)
