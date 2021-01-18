package asura.core.util

object HostUtils {

  val hostname = try {
    import scala.sys.process._
    "hostname".!!.trim
  } catch {
    case _: Throwable => "Unknown"
  }

}
