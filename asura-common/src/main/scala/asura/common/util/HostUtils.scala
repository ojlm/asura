package asura.common.util

object HostUtils {

  val hostname: String = try {
    import scala.sys.process._
    "hostname".!!.trim
  } catch {
    case _: Throwable => "Unknown"
  }

}
