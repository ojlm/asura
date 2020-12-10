package asura.ui.driver

case class UiDriverAddress(
                            host: String,
                            port: Int,
                            password: String,
                            `type`: String,
                          )

object UiDriverAddress {

  val DRIVER_TYPE_CHROME = "chrome"

}
