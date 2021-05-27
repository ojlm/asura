package asura.ui.cli.hub

import asura.ui.cli.codec.{Point, Size}
import asura.ui.cli.hub.ControlMessage.Position

case class ControlMessage(`type`: Int) {
  var text: String = null
  var metaState: Int = 0 // KeyEvent.META_*
  var action: Int = 0 // KeyEvent.ACTION_* or MotionEvent.ACTION_* or POWER_MODE_*
  var keycode: Int = 0 // KeyEvent.KEYCODE_*
  var buttons: Int = 0 // MotionEvent.BUTTON_*
  var pointerId: Long = 0L
  var pressure: Float = 0.0F
  var position: Position = null
  var hScroll: Int = 0
  var vScroll: Int = 0
  var paste: Boolean = false
  var repeat: Int = 0
}

object ControlMessage {

  val TYPE_INJECT_KEYCODE = 0
  val TYPE_INJECT_TEXT = 1
  val TYPE_INJECT_TOUCH_EVENT = 2
  val TYPE_INJECT_SCROLL_EVENT = 3
  val TYPE_BACK_OR_SCREEN_ON = 4
  val TYPE_EXPAND_NOTIFICATION_PANEL = 5
  val TYPE_COLLAPSE_NOTIFICATION_PANEL = 6
  val TYPE_GET_CLIPBOARD = 7
  val TYPE_SET_CLIPBOARD = 8
  val TYPE_SET_SCREEN_POWER_MODE = 9
  val TYPE_ROTATE_DEVICE = 10

  // 1 byte type + payload
  val INJECT_KEYCODE_LENGTH = 1 + 13
  val INJECT_TOUCH_EVENT_LENGTH = 1 + 27
  val INJECT_SCROLL_EVENT_LENGTH = 1 + 20
  val SET_SCREEN_POWER_MODE_LENGTH = 1 + 1
  val SET_CLIPBOARD_FIXED_LENGTH = 1 + 1
  val MESSAGE_MAX_SIZE = 1 << 18 // 256k
  val CLIPBOARD_TEXT_MAX_LENGTH = MESSAGE_MAX_SIZE - 6 // type: 1 byte; paste flag: 1 byte; length: 4 bytes
  val INJECT_TEXT_MAX_LENGTH = 300

  // The video screen size may be different from the real device screen size,
  // so store to which size the absolute position apply, to scale it
  // accordingly.
  case class Position(point: Point, screenSize: Size)

  def ofType(`type`: Int) = ControlMessage(`type`)

  def ofKeycode(action: Int, keycode: Int, repeat: Int, metaState: Int): ControlMessage = {
    val msg = ControlMessage(TYPE_INJECT_KEYCODE)
    msg.action = action
    msg.keycode = keycode
    msg.repeat = repeat
    msg.metaState = metaState
    msg
  }

  def ofText(text: String): ControlMessage = {
    val msg = ControlMessage(TYPE_INJECT_TEXT)
    msg.text = text
    msg
  }

  def ofTouchEvent(action: Int, pointerId: Long, position: Position, pressure: Float, buttons: Int): ControlMessage = {
    val msg = ControlMessage(TYPE_INJECT_TOUCH_EVENT)
    msg.action = action
    msg.pointerId = pointerId
    msg.pressure = pressure
    msg.position = position
    msg.buttons = buttons
    msg
  }

  def ofScrollEvent(position: Position, hScroll: Int, vScroll: Int): ControlMessage = {
    val msg = ControlMessage(TYPE_INJECT_SCROLL_EVENT)
    msg.position = position
    msg.hScroll = hScroll
    msg.vScroll = vScroll
    msg
  }

  def ofSetClipboard(text: String, paste: Boolean): ControlMessage = {
    val msg = ControlMessage(TYPE_SET_CLIPBOARD)
    msg.text = text
    msg.paste = paste
    msg
  }

  def ofSetScreenPowerMode(mode: Int): ControlMessage = {
    val msg = ControlMessage(TYPE_SET_SCREEN_POWER_MODE)
    msg.action = mode
    msg
  }

}
