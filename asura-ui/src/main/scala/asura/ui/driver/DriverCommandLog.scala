package asura.ui.driver

/**
 * @param command which command
 * @param `type`  subtype of command
 * @param params  log data
 */
case class DriverCommandLog(command: String, `type`: String, params: Any)
