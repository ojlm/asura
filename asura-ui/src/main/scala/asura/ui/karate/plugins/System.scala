package asura.ui.karate.plugins

import java.awt._
import java.awt.event.{InputEvent, KeyEvent}
import java.util

import javax.swing.{BorderFactory, JComponent, JFrame, WindowConstants}

import asura.ui.jna.WindowUtils
import asura.ui.karate.plugins.System.{KEY_CODES, logger}
import asura.ui.model.{IntPoint, Position}
import asura.ui.opencv.OpenCvUtils
import com.intuit.karate.core.{AutoDef, Plugin}
import com.intuit.karate.driver.{Driver, Keys}
import com.intuit.karate.http.ResourceType
import com.typesafe.scalalogging.Logger
import oshi.software.os.OSDesktopWindow

class System(val driver: Driver, val ocr: Ocr, val img: Img, autoDelay: Boolean = true) extends CvPlugin {

  private var bot: Robot = null
  private var kit: Toolkit = null
  var window: WindowElement = null

  override def getRootPosition(): Position = {
    init()
    if (window != null) {
      // if a window is activated, the position coordinate is in the window
      val rect = window.getWindowRect()
      Position(0, 0, rect.width, rect.height)
    } else {
      val size = kit.getScreenSize
      Position(0, 0, size.width, size.height)
    }
  }

  def getAbsolutePoint(x: Int, y: Int): IntPoint = {
    if (window != null) {
      val rect = window.getWindowRect()
      IntPoint(rect.x + x, rect.y + y)
    } else {
      IntPoint(x, y)
    }
  }

  @AutoDef
  def screenInfo(): util.List[util.Map[String, Object]] = {
    val list = new util.ArrayList[util.Map[String, Object]]()
    GraphicsEnvironment.getLocalGraphicsEnvironment.getScreenDevices.foreach(device => {
      val map = new util.HashMap[String, Object]()
      val mode = device.getDisplayMode
      map.put("mode", mode)
      val bounds = device.getDefaultConfiguration.getBounds
      map.put("bounds", s"[x=${bounds.x},y=${bounds.y},width=${bounds.width},height=${bounds.height}]")
      list.add(map)
    })
    list
  }

  @AutoDef
  def windowInfo(title: String): util.List[util.Map[String, Object]] = {
    val list = new util.ArrayList[util.Map[String, Object]]()
    WindowUtils.getDesktopWindows(title).forEach(window => {
      val map = new util.HashMap[String, Object]()
      map.put("title", window.getTitle)
      map.put("command", window.getCommand)
      map.put("windowId", Long.box(window.getWindowId))
      map.put("pid", Long.box(window.getOwningProcessId))
      val rect = window.getLocAndSize
      map.put("x", Int.box(rect.x))
      map.put("y", Int.box(rect.y))
      map.put("width", Int.box(rect.width))
      map.put("height", Int.box(rect.height))
      map.put("visible", Boolean.box(window.isVisible))
      list.add(map)
    })
    list
  }

  @AutoDef
  def activate(pid: Long): System = {
    activate(pid, 0)
  }

  @AutoDef
  def activate(pid: Long, idx: Int): System = {
    activate(WindowUtils.getDesktopWindow(pid), idx, null, pid)
  }

  @AutoDef
  def activate(title: String): System = {
    activate(title, 0)
  }

  @AutoDef
  def activate(title: String, idx: Int): System = {
    activate(WindowUtils.getDesktopWindows(title), idx, title, -1)
  }

  def activate(windows: java.util.List[OSDesktopWindow], idx: Int, title: String, pid: Long): System = {
    if (windows != null && windows.size() > idx) {
      val w = windows.get(idx)
      window = WindowElement(w.getWindowId, title, w.getOwningProcessId)
      this
    } else {
      throw new RuntimeException(s"can not find window ${if (title != null) title else pid}")
    }
  }

  @AutoDef
  def inactivate(): System = {
    window = null
    this
  }

  @AutoDef
  def screenshot(): Array[Byte] = {
    screenshot(true)
  }

  @AutoDef
  def screenshot(embed: Boolean): Array[Byte] = {
    val pos = getRootPosition()
    screenshot(pos.x, pos.y, pos.width, pos.height, embed)
  }

  @AutoDef
  def screenshot(x: Int, y: Int, width: Int, height: Int): Array[Byte] = {
    screenshot(x, y, width, height, true)
  }

  @AutoDef
  def screenshot(x: Int, y: Int, width: Int, height: Int, embed: Boolean): Array[Byte] = {
    init()
    val point = getAbsolutePoint(x, y)
    val image = bot.createScreenCapture(new Rectangle(point.x, point.y, width, height))
    val bytes = OpenCvUtils.toBytes(image)
    if (embed) {
      driver.getRuntime.embed(bytes, ResourceType.PNG)
    }
    bytes
  }

  @AutoDef
  def crop(): ImageElement = {
    ImageElement(null, getRootPosition(), driver, ocr, img, this)
  }

  @AutoDef
  def crop(locator: Object): ImageElement = {
    ImageElement(crop(), Position(locator, getRootPosition()), driver, ocr, img, this)
  }

  @AutoDef
  def crop(x: Object, y: Object): ImageElement = {
    ImageElement(crop(), Position(x, y, getRootPosition()), driver, ocr, img, this)
  }

  @AutoDef
  def crop(x: Object, y: Object, width: Object, height: Object): Element = {
    ImageElement(crop(), Position(x, y, width, height, getRootPosition()), driver, ocr, img, this)
  }

  @AutoDef
  def move(x: Int, y: Int): System = {
    init()
    val point = getAbsolutePoint(x, y)
    bot.mouseMove(point.x, point.y)
    this
  }

  @AutoDef
  def click(): System = {
    click(1)
  }

  @AutoDef
  def midClick(): System = {
    click(2)
  }

  @AutoDef
  def rightClick(): System = {
    click(3)
  }

  @AutoDef
  def click(num: Int): System = {
    init()
    val mask = InputEvent.getMaskForButton(num)
    bot.mousePress(mask)
    bot.mouseRelease(mask)
    this
  }

  @AutoDef
  def doubleClick(): System = {
    click()
    delay(40)
    click()
    this
  }

  @AutoDef
  def click(x: Int, y: Int): System = {
    move(x, y).click()
  }

  @AutoDef
  def press(): System = {
    init()
    bot.mousePress(InputEvent.getMaskForButton(1))
    this
  }

  @AutoDef
  def release(): System = {
    init()
    bot.mouseRelease(InputEvent.getMaskForButton(1))
    this
  }

  @AutoDef
  def input(values: Array[String]): System = {
    input(values, 0)
  }

  @AutoDef
  def input(text: String, delay: Int): System = {
    val values = new Array[String](text.length)
    for (i <- 0 until text.length) {
      values(i) = Character.toString(text.charAt(i))
    }
    input(values, delay)
  }

  @AutoDef
  def input(values: Array[String], millis: Int): System = {
    values.foreach(value => {
      if (millis > 0) delay(millis)
      input(value)
    })
    this
  }

  @AutoDef
  def input(value: String): System = {
    init()
    val sb = new StringBuilder()
    value.foreach(c => {
      if (Keys.isModifier(c)) {
        sb.append(c)
        val codes = KEY_CODES.get(c)
        if (codes == null) {
          logger.warn(s"cannot resolve char: $c")
          bot.keyPress(c)
        } else {
          bot.keyPress(codes(0))
        }
      } else {
        val codes = KEY_CODES.get(c)
        if (codes == null) {
          logger.warn(s"cannot resolve char: $c")
          bot.keyPress(c)
          bot.keyRelease(c)
        } else if (codes.length > 1) {
          bot.keyPress(codes(0))
          bot.keyPress(codes(1))
          bot.keyRelease(codes(0))
          bot.keyRelease(codes(1))
        } else {
          bot.keyPress(codes(0))
          bot.keyRelease(codes(0))
        }
      }
    })
    sb.foreach(c => { // modifiers
      val codes = KEY_CODES.get(c)
      if (codes == null) {
        logger.warn(s"cannot resolve char: $c")
        bot.keyRelease(c)
      } else {
        bot.keyRelease(codes(0))
      }
    })
    this
  }

  @AutoDef
  def clearFocused(): System = {
    input(s"${Keys.CONTROL}a${Keys.DELETE}")
  }

  @AutoDef
  def delay(millis: Int): System = {
    bot.delay(millis)
    this
  }

  @AutoDef
  def highlight(pos: Position): System = {
    highlight(pos.x, pos.y, pos.width, pos.height, 1000)
  }

  @AutoDef
  def highlight(pos: Position, time: Int): System = {
    highlight(pos.x, pos.y, pos.width, pos.height, time)
  }

  @AutoDef
  def highlight(x: Int, y: Int, width: Int, height: Int): System = {
    highlight(x, y, width, height, 1000)
  }

  @AutoDef
  def highlight(x: Int, y: Int, width: Int, height: Int, time: Int): System = {
    val point = getAbsolutePoint(x, y)
    System.highlight(Position(point.x, point.y, width, height), time)
    this
  }

  @AutoDef
  def highlightAll(container: Position, elements: Seq[Position], time: Int, showValue: Boolean): System = {
    val point = getAbsolutePoint(container.x, container.y)
    container.x = point.x
    container.y = point.y
    System.highlightAll(container, elements, time, showValue)
    this
  }

  @AutoDef
  def robot: Robot = {
    init()
    bot
  }

  @AutoDef
  def toolkit: Toolkit = {
    init()
    kit
  }

  private def init(): Unit = {
    if (bot == null) {
      try {
        bot = new Robot()
        bot.setAutoWaitForIdle(autoDelay)
        bot.setAutoWaitForIdle(true)
      } catch {
        case t: Throwable => throw new RuntimeException(s"Native system robot init failed", t)
      }
    }
    if (kit == null) {
      try {
        kit = Toolkit.getDefaultToolkit
      } catch {
        case t: Throwable => throw new RuntimeException(s"Native system toolkit init failed", t)
      }
    }
  }

  override def methodNames(): util.List[String] = System.METHOD_NAMES

}

object System {

  val logger = Logger("SYS")
  val ENGINE_KEY = "sys"
  val METHOD_NAMES: util.List[String] = Plugin.methodNames(classOf[System])
  val KEY_CODES = new util.HashMap[Character, Seq[Int]]()

  def highlight(pos: Position, time: Int): Unit = {
    val frame = createFrame()
    frame.setLocation(pos.x, pos.y)
    frame.setSize(pos.width, pos.height)
    frame.getRootPane.setBorder(BorderFactory.createLineBorder(Color.RED, 3))
    frame.setVisible(true)
    delay(time)
    frame.dispose()
  }

  def highlightAll(container: Position, elements: Seq[Position], time: Int, showValue: Boolean = false): Unit = {
    val frame = createFrame()
    frame.setLocation(container.x, container.y)
    frame.setSize(container.width, container.height)
    frame.getRootPane.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3))
    val boxes = elements.filter(pos => {
      val x = pos.x - container.x
      val y = pos.y - container.y
      if (x > 0 && y > 0 && pos.width > 0 && pos.height > 0) {
        pos.x = x
        pos.y = y
        true
      } else {
        false
      }
    })
    frame.add(new JComponent {
      override def paintComponent(g: Graphics): Unit = {
        super.paintComponent(g)
        val g2d = g.asInstanceOf[Graphics2D]
        g2d.setStroke(new BasicStroke(2))
        boxes.foreach(pos => {
          g.setColor(Color.RED)
          g.drawRect(pos.x, pos.y, pos.width, pos.height)
          if (showValue && pos.value != null) {
            g.setColor(Color.BLACK)
            g.drawString(pos.value.toString, pos.x, pos.y)
          }
        })
      }
    })
    frame.setVisible(true)
    delay(time)
    frame.dispose()
  }

  def delay(millis: Int): Unit = {
    try {
      Thread.sleep(millis)
    } catch {
      case t: Throwable => throw new RuntimeException(t)
    }
  }

  private def createFrame(): JFrame = {
    val frame = new JFrame()
    frame.setUndecorated(true)
    frame.setBackground(new Color(0, 0, 0, 0))
    frame.setAlwaysOnTop(true)
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.setType(Window.Type.UTILITY)
    frame.setFocusableWindowState(false)
    frame.setAutoRequestFocus(false)
    frame
  }

  private def key(char: Character, codes: Int*): Unit = {
    KEY_CODES.put(char, codes)
  }
  {
    key('a', KeyEvent.VK_A);
    key('b', KeyEvent.VK_B)
    key('c', KeyEvent.VK_C)
    key('d', KeyEvent.VK_D)
    key('e', KeyEvent.VK_E)
    key('f', KeyEvent.VK_F)
    key('g', KeyEvent.VK_G)
    key('h', KeyEvent.VK_H)
    key('i', KeyEvent.VK_I)
    key('j', KeyEvent.VK_J)
    key('k', KeyEvent.VK_K)
    key('l', KeyEvent.VK_L)
    key('m', KeyEvent.VK_M)
    key('n', KeyEvent.VK_N)
    key('o', KeyEvent.VK_O)
    key('p', KeyEvent.VK_P)
    key('q', KeyEvent.VK_Q)
    key('r', KeyEvent.VK_R)
    key('s', KeyEvent.VK_S)
    key('t', KeyEvent.VK_T)
    key('u', KeyEvent.VK_U)
    key('v', KeyEvent.VK_V)
    key('w', KeyEvent.VK_W)
    key('x', KeyEvent.VK_X)
    key('y', KeyEvent.VK_Y)
    key('z', KeyEvent.VK_Z)
    key('A', KeyEvent.VK_SHIFT, KeyEvent.VK_A)
    key('B', KeyEvent.VK_SHIFT, KeyEvent.VK_B)
    key('C', KeyEvent.VK_SHIFT, KeyEvent.VK_C)
    key('D', KeyEvent.VK_SHIFT, KeyEvent.VK_D)
    key('E', KeyEvent.VK_SHIFT, KeyEvent.VK_E)
    key('F', KeyEvent.VK_SHIFT, KeyEvent.VK_F)
    key('G', KeyEvent.VK_SHIFT, KeyEvent.VK_G)
    key('H', KeyEvent.VK_SHIFT, KeyEvent.VK_H)
    key('I', KeyEvent.VK_SHIFT, KeyEvent.VK_I)
    key('J', KeyEvent.VK_SHIFT, KeyEvent.VK_J)
    key('K', KeyEvent.VK_SHIFT, KeyEvent.VK_K)
    key('L', KeyEvent.VK_SHIFT, KeyEvent.VK_L)
    key('M', KeyEvent.VK_SHIFT, KeyEvent.VK_M)
    key('N', KeyEvent.VK_SHIFT, KeyEvent.VK_N)
    key('O', KeyEvent.VK_SHIFT, KeyEvent.VK_O)
    key('P', KeyEvent.VK_SHIFT, KeyEvent.VK_P)
    key('Q', KeyEvent.VK_SHIFT, KeyEvent.VK_Q)
    key('R', KeyEvent.VK_SHIFT, KeyEvent.VK_R)
    key('S', KeyEvent.VK_SHIFT, KeyEvent.VK_S)
    key('T', KeyEvent.VK_SHIFT, KeyEvent.VK_T)
    key('U', KeyEvent.VK_SHIFT, KeyEvent.VK_U)
    key('V', KeyEvent.VK_SHIFT, KeyEvent.VK_V)
    key('W', KeyEvent.VK_SHIFT, KeyEvent.VK_W)
    key('X', KeyEvent.VK_SHIFT, KeyEvent.VK_X)
    key('Y', KeyEvent.VK_SHIFT, KeyEvent.VK_Y)
    key('Z', KeyEvent.VK_SHIFT, KeyEvent.VK_Z)
    key('1', KeyEvent.VK_1)
    key('2', KeyEvent.VK_2)
    key('3', KeyEvent.VK_3)
    key('4', KeyEvent.VK_4)
    key('5', KeyEvent.VK_5)
    key('6', KeyEvent.VK_6)
    key('7', KeyEvent.VK_7)
    key('8', KeyEvent.VK_8)
    key('9', KeyEvent.VK_9)
    key('0', KeyEvent.VK_0)
    key('!', KeyEvent.VK_SHIFT, KeyEvent.VK_1)
    key('@', KeyEvent.VK_SHIFT, KeyEvent.VK_2)
    key('#', KeyEvent.VK_SHIFT, KeyEvent.VK_3)
    key('$', KeyEvent.VK_SHIFT, KeyEvent.VK_4)
    key('%', KeyEvent.VK_SHIFT, KeyEvent.VK_5)
    key('^', KeyEvent.VK_SHIFT, KeyEvent.VK_6)
    key('&', KeyEvent.VK_SHIFT, KeyEvent.VK_7)
    key('*', KeyEvent.VK_SHIFT, KeyEvent.VK_8)
    key('(', KeyEvent.VK_SHIFT, KeyEvent.VK_9)
    key(')', KeyEvent.VK_SHIFT, KeyEvent.VK_0)
    key('`', KeyEvent.VK_BACK_QUOTE)
    key('~', KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_QUOTE)
    key('-', KeyEvent.VK_MINUS)
    key('_', KeyEvent.VK_SHIFT, KeyEvent.VK_MINUS)
    key('=', KeyEvent.VK_EQUALS)
    key('+', KeyEvent.VK_SHIFT, KeyEvent.VK_EQUALS)
    key('[', KeyEvent.VK_OPEN_BRACKET)
    key('{', KeyEvent.VK_SHIFT, KeyEvent.VK_OPEN_BRACKET)
    key(']', KeyEvent.VK_CLOSE_BRACKET)
    key('}', KeyEvent.VK_SHIFT, KeyEvent.VK_CLOSE_BRACKET)
    key('\\', KeyEvent.VK_BACK_SLASH)
    key('|', KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_SLASH)
    key(';', KeyEvent.VK_SEMICOLON)
    key(':', KeyEvent.VK_SHIFT, KeyEvent.VK_SEMICOLON)
    key('\'', KeyEvent.VK_QUOTE)
    key('"', KeyEvent.VK_SHIFT, KeyEvent.VK_QUOTE)
    key(',', KeyEvent.VK_COMMA)
    key('<', KeyEvent.VK_SHIFT, KeyEvent.VK_COMMA)
    key('.', KeyEvent.VK_PERIOD)
    key('|', KeyEvent.VK_SHIFT, KeyEvent.VK_PERIOD)
    key('/', KeyEvent.VK_SLASH)
    key('?', KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH)
    //=====================================================================
    key('\b', KeyEvent.VK_BACK_SPACE)
    key('\t', KeyEvent.VK_TAB)
    key('\r', KeyEvent.VK_ENTER)
    key('\n', KeyEvent.VK_ENTER)
    key(' ', KeyEvent.VK_SPACE)
    key(Keys.CONTROL, KeyEvent.VK_CONTROL)
    key(Keys.ALT, KeyEvent.VK_ALT)
    key(Keys.META, KeyEvent.VK_META)
    key(Keys.SHIFT, KeyEvent.VK_SHIFT)
    key(Keys.TAB, KeyEvent.VK_TAB)
    key(Keys.ENTER, KeyEvent.VK_ENTER)
    key(Keys.SPACE, KeyEvent.VK_SPACE)
    key(Keys.BACK_SPACE, KeyEvent.VK_BACK_SPACE)
    //=====================================================================
    key(Keys.UP, KeyEvent.VK_UP)
    key(Keys.RIGHT, KeyEvent.VK_RIGHT)
    key(Keys.DOWN, KeyEvent.VK_DOWN)
    key(Keys.LEFT, KeyEvent.VK_LEFT)
    key(Keys.PAGE_UP, KeyEvent.VK_PAGE_UP)
    key(Keys.PAGE_DOWN, KeyEvent.VK_PAGE_DOWN)
    key(Keys.END, KeyEvent.VK_END)
    key(Keys.HOME, KeyEvent.VK_HOME)
    key(Keys.DELETE, KeyEvent.VK_DELETE)
    key(Keys.ESCAPE, KeyEvent.VK_ESCAPE)
    key(Keys.F1, KeyEvent.VK_F1)
    key(Keys.F2, KeyEvent.VK_F2)
    key(Keys.F3, KeyEvent.VK_F3)
    key(Keys.F4, KeyEvent.VK_F4)
    key(Keys.F5, KeyEvent.VK_F5)
    key(Keys.F6, KeyEvent.VK_F6)
    key(Keys.F7, KeyEvent.VK_F7)
    key(Keys.F8, KeyEvent.VK_F8)
    key(Keys.F9, KeyEvent.VK_F9)
    key(Keys.F10, KeyEvent.VK_F10)
    key(Keys.F11, KeyEvent.VK_F11)
    key(Keys.F12, KeyEvent.VK_F12)
    key(Keys.INSERT, KeyEvent.VK_INSERT)
    key(Keys.PAUSE, KeyEvent.VK_PAUSE)
    key(Keys.NUMPAD1, KeyEvent.VK_NUMPAD1)
    key(Keys.NUMPAD2, KeyEvent.VK_NUMPAD2)
    key(Keys.NUMPAD3, KeyEvent.VK_NUMPAD3)
    key(Keys.NUMPAD4, KeyEvent.VK_NUMPAD4)
    key(Keys.NUMPAD5, KeyEvent.VK_NUMPAD5)
    key(Keys.NUMPAD6, KeyEvent.VK_NUMPAD6)
    key(Keys.NUMPAD7, KeyEvent.VK_NUMPAD7)
    key(Keys.NUMPAD8, KeyEvent.VK_NUMPAD8)
    key(Keys.NUMPAD9, KeyEvent.VK_NUMPAD9)
    key(Keys.NUMPAD0, KeyEvent.VK_NUMPAD0)
    key(Keys.SEPARATOR, KeyEvent.VK_SEPARATOR)
    key(Keys.ADD, KeyEvent.VK_ADD)
    key(Keys.SUBTRACT, KeyEvent.VK_SUBTRACT)
    key(Keys.MULTIPLY, KeyEvent.VK_MULTIPLY)
    key(Keys.DIVIDE, KeyEvent.VK_DIVIDE)
    key(Keys.DECIMAL, KeyEvent.VK_DECIMAL)
    // TODO SCROLL_LOCK, NUM_LOCK, CAPS_LOCK, PRINTSCREEN, CONTEXT_MENU, WINDOWS
  }

}
