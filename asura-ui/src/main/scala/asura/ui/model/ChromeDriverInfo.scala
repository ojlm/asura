package asura.ui.model

case class ChromeDriverInfo(
                             host: String,
                             port: Int,
                             password: String,
                           ) extends DriverInfo {

}
