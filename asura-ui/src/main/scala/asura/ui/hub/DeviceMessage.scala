package asura.ui.hub

case class DeviceMessage(`type`: Int) {
  var text: String = null
}

object DeviceMessage {

  val TYPE_CLIPBOARD = 0

  val MESSAGE_MAX_SIZE = 1 << 18 // 256k
  val CLIPBOARD_TEXT_MAX_LENGTH: Int = MESSAGE_MAX_SIZE - 5 // type: 1 byte; length: 4 bytes

  def ofClipboard(text: String): DeviceMessage = {
    val msg = DeviceMessage(TYPE_CLIPBOARD)
    msg.text = text
    msg
  }

}
