package asura.ui.model

case class ChromeDriverInfo(
                             host: String,
                             port: Int,
                             password: String,
                             var targets: Seq[ChromeTargetPage] = Nil,
                             var version: ChromeVersion = null,
                           ) extends DriverInfo {

}
