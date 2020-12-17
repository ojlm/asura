package asura.ui.driver

case class DriverCommandEnd(
                             `type`: String,
                             ok: Boolean,
                             msg: String = null,
                             result: Any = null,
                           )
