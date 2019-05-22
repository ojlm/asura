package asura.common.exceptions

case class WithDataException(t: Throwable, data: Any = null) extends RuntimeException(t)
