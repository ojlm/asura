package asura.ui.driver

case class DriverCommandEnd(
                             ok: Boolean,
                             msg: String = null,
                             result: Any = null,
                           )
