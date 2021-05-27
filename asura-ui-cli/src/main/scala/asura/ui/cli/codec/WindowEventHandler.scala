package asura.ui.cli.codec

import asura.ui.cli.codec.WindowEventHandler._
import asura.ui.cli.codec.android.AndroidInput.{AndroidKeyEventAction, AndroidMetaState, AndroidMotionEventAction, AndroidMotionEventButtons}
import asura.ui.cli.codec.android.AndroidKeyCodes
import asura.ui.cli.hub.ControlMessage
import asura.ui.cli.hub.ControlMessage.Position
import asura.ui.cli.hub.Hubs.ControllerHub
import com.typesafe.scalalogging.Logger
import javafx.event.{Event, EventHandler}
import javafx.scene.Node
import javafx.scene.input.{KeyCode, KeyEvent, MouseEvent, ScrollEvent}

case class WindowEventHandler(device: String, frameSize: Size) extends EventHandler[Event] {

  val sinks = ControllerHub.getSinks(device)
  var lastScrollTime = System.nanoTime()

  override def handle(event: Event): Unit = {
    event match {
      case event: MouseEvent => handleMouseEvent(event)
      case event: KeyEvent => handleKeyEvent(event)
      case event: ScrollEvent => handleScrollEvent(event)
      case _ => logger.info(s"[$device] ignore event: ${event.getEventType.getName}")
    }
  }

  def handleMouseEvent(event: MouseEvent): Unit = {
    val action = event.getEventType match {
      case MouseEvent.MOUSE_PRESSED => AndroidMotionEventAction.AMOTION_EVENT_ACTION_DOWN
      case MouseEvent.MOUSE_DRAGGED => AndroidMotionEventAction.AMOTION_EVENT_ACTION_MOVE
      case MouseEvent.MOUSE_RELEASED => AndroidMotionEventAction.AMOTION_EVENT_ACTION_UP
      case _ => FAIL
    }
    if (action != FAIL) {
      val node = event.getTarget.asInstanceOf[Node]
      val position = convertToFramePosition(event.getX, event.getY, node, frameSize)
      val pressure = if (action == AndroidMotionEventAction.AMOTION_EVENT_ACTION_UP) 0.0F else 1.0F
      var buttons = 0
      if (event.isPrimaryButtonDown) {
        buttons = buttons | AndroidMotionEventButtons.AMOTION_EVENT_BUTTON_PRIMARY
      }
      if (event.isSecondaryButtonDown) {
        buttons = buttons | AndroidMotionEventButtons.AMOTION_EVENT_BUTTON_SECONDARY
      }
      if (event.isMiddleButtonDown) {
        buttons = buttons | AndroidMotionEventButtons.AMOTION_EVENT_BUTTON_TERTIARY
      }
      if (event.isBackButtonDown) {
        buttons = buttons | AndroidMotionEventButtons.AMOTION_EVENT_BUTTON_BACK
      }
      if (event.isForwardButtonDown) {
        buttons = buttons | AndroidMotionEventButtons.AMOTION_EVENT_BUTTON_FORWARD
      }
      val message = ControlMessage.ofTouchEvent(action, POINTER_ID_MOUSE, position, pressure, buttons)
      ControllerHub.write(sinks, message)
    }
  }

  def handleKeyEvent(event: KeyEvent): Unit = {
    if (event.isShortcutDown) {
      // shortcut functions
      // TODO
    } else {
      val action = event.getEventType match {
        case KeyEvent.KEY_PRESSED => AndroidKeyEventAction.AKEY_EVENT_ACTION_DOWN
        case KeyEvent.KEY_RELEASED => AndroidKeyEventAction.AKEY_EVENT_ACTION_UP
        case _ => FAIL
      }
      if (action != FAIL) {
        val keycode = convertKeyCode(event)
        if (keycode != FAIL) {
          val metaState = convertMetaState(event)
          val message = ControlMessage.ofKeycode(action, keycode, 0, metaState)
          ControllerHub.write(sinks, message)
        }
      }
    }
  }

  def handleScrollEvent(event: ScrollEvent): Unit = {
    val now = System.nanoTime()
    if (now - lastScrollTime > 20_000_000) { // > 20ms for debounce
      lastScrollTime = now
      val deltaX = -event.getDeltaX.toInt
      val deltaY = event.getDeltaY.toInt
      if (deltaX != 0 && deltaY != 0) {
        val node = event.getTarget.asInstanceOf[Node]
        val position = convertToFramePosition(event.getX, event.getY, node, frameSize)
        val message = ControlMessage.ofScrollEvent(position, deltaX, deltaY)
        ControllerHub.write(sinks, message)
      }
    }
  }

}

object WindowEventHandler {

  val logger = Logger(getClass)
  val FAIL = -1
  val POINTER_ID_MOUSE = -1L
  val POINTER_ID_VIRTUAL_FINGER = -2L

  def convertMetaState(event: KeyEvent): Int = {
    var metaState = 0
    if (event.isShiftDown) {
      metaState = metaState | AndroidMetaState.AMETA_SHIFT_ON
    }
    if (event.isControlDown) {
      metaState = metaState | AndroidMetaState.AMETA_CTRL_ON
    }
    if (event.isAltDown) {
      metaState = metaState | AndroidMetaState.AMETA_ALT_ON
    }
    if (event.isMetaDown) {
      metaState = metaState | AndroidMetaState.AMETA_META_ON
    }
    metaState
  }

  def convertKeyCode(event: KeyEvent): Int = {
    val keycode = event.getCode
    keycode match {
      case KeyCode.ENTER => if (keycode.isKeypadKey) AndroidKeyCodes.KEYCODE_NUMPAD_ENTER else AndroidKeyCodes.KEYCODE_ENTER
      case KeyCode.ESCAPE => AndroidKeyCodes.KEYCODE_ESCAPE
      case KeyCode.BACK_SPACE => AndroidKeyCodes.KEYCODE_DEL
      case KeyCode.TAB => AndroidKeyCodes.KEYCODE_TAB
      case KeyCode.PAGE_UP => AndroidKeyCodes.KEYCODE_PAGE_UP
      case KeyCode.DELETE => AndroidKeyCodes.KEYCODE_FORWARD_DEL
      case KeyCode.HOME => AndroidKeyCodes.KEYCODE_MOVE_HOME
      case KeyCode.END => AndroidKeyCodes.KEYCODE_MOVE_END
      case KeyCode.PAGE_DOWN => AndroidKeyCodes.KEYCODE_PAGE_DOWN
      case KeyCode.RIGHT | KeyCode.KP_RIGHT => AndroidKeyCodes.KEYCODE_DPAD_RIGHT
      case KeyCode.LEFT | KeyCode.KP_LEFT => AndroidKeyCodes.KEYCODE_DPAD_LEFT
      case KeyCode.DOWN | KeyCode.KP_DOWN => AndroidKeyCodes.KEYCODE_DPAD_DOWN
      case KeyCode.UP | KeyCode.KP_UP => AndroidKeyCodes.KEYCODE_DPAD_UP
      case KeyCode.CONTROL => AndroidKeyCodes.KEYCODE_CTRL_LEFT
      case KeyCode.SHIFT => AndroidKeyCodes.KEYCODE_SHIFT_LEFT
      // handle letters and space
      case KeyCode.A => AndroidKeyCodes.KEYCODE_A
      case KeyCode.B => AndroidKeyCodes.KEYCODE_B
      case KeyCode.C => AndroidKeyCodes.KEYCODE_C
      case KeyCode.D => AndroidKeyCodes.KEYCODE_D
      case KeyCode.E => AndroidKeyCodes.KEYCODE_E
      case KeyCode.F => AndroidKeyCodes.KEYCODE_F
      case KeyCode.G => AndroidKeyCodes.KEYCODE_G
      case KeyCode.H => AndroidKeyCodes.KEYCODE_H
      case KeyCode.I => AndroidKeyCodes.KEYCODE_I
      case KeyCode.J => AndroidKeyCodes.KEYCODE_J
      case KeyCode.K => AndroidKeyCodes.KEYCODE_K
      case KeyCode.L => AndroidKeyCodes.KEYCODE_L
      case KeyCode.M => AndroidKeyCodes.KEYCODE_M
      case KeyCode.N => AndroidKeyCodes.KEYCODE_N
      case KeyCode.O => AndroidKeyCodes.KEYCODE_O
      case KeyCode.P => AndroidKeyCodes.KEYCODE_P
      case KeyCode.Q => AndroidKeyCodes.KEYCODE_Q
      case KeyCode.R => AndroidKeyCodes.KEYCODE_R
      case KeyCode.S => AndroidKeyCodes.KEYCODE_S
      case KeyCode.T => AndroidKeyCodes.KEYCODE_T
      case KeyCode.U => AndroidKeyCodes.KEYCODE_U
      case KeyCode.V => AndroidKeyCodes.KEYCODE_V
      case KeyCode.W => AndroidKeyCodes.KEYCODE_W
      case KeyCode.X => AndroidKeyCodes.KEYCODE_X
      case KeyCode.Y => AndroidKeyCodes.KEYCODE_Y
      case KeyCode.Z => AndroidKeyCodes.KEYCODE_Z
      case KeyCode.SPACE => AndroidKeyCodes.KEYCODE_SPACE
      case _ => FAIL
    }
  }

  def convertToFrameCoords(x: Double, y: Double, w: Double, h: Double, frameSize: Size): Point = {
    val toX = x * frameSize.width / w
    val toY = y * frameSize.height / h
    Point(toX.toInt, toY.toInt)
  }

  def convertToFramePosition(x: Double, y: Double, view: Node, frameSize: Size): Position = {
    val bounds = view.getBoundsInLocal
    val point = convertToFrameCoords(x, y, bounds.getWidth, bounds.getHeight, frameSize)
    Position(point, frameSize)
  }

}
