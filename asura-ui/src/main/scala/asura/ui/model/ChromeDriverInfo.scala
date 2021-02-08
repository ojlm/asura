package asura.ui.model

case class ChromeDriverInfo(
                             host: String,
                             port: Int,
                             password: String,
                             var targets: Seq[ChromeTargetPage] = Nil,
                           ) extends DriverInfo {

}
