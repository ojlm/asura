package asura.ui.driver

/**
 * @param command which command
 * @param `type`  subtype of command
 */
case class DriverCommandLog(
                             command: String,
                             `type`: String,
                             params: Object,
                             meta: CommandMeta,
                             timestamp: Long = System.currentTimeMillis(),
                           )
