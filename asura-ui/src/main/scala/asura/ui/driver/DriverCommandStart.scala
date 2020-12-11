package asura.ui.driver

case class DriverCommandStart(
                               ok: Boolean,
                               msg: String,
                               status: DriverStatus,
                             )
