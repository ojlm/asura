package asura.ui.model

case class ChromeDriverInfo(
                             host: String,
                             port: Int,
                             password: String,
                             electron: Boolean = false,
                             var targets: Seq[ChromeTargetPage] = Nil,
                             var version: ChromeVersion = null,
                             var startUrl: String = null,
                             var debuggerUrl: String = null,
                           ) extends DriverInfo {

}
