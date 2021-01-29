package asura.ui.model

case class ChromeDriverInfo(
                             host: String,
                             port: Int,
                             password: String,
                             var screenCapture: String = null,
                           ) extends DriverInfo {

}
