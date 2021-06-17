package asura.ui.message

import asura.common.util.JsonUtils
import asura.ui.annotation.Required
import asura.ui.message.wd.HttpRequest
import com.fasterxml.jackson.annotation.{JsonAlias, JsonProperty}
import com.typesafe.scalalogging.Logger

package object builder {

  val logger = Logger("MessageBuilder")

  val SESSION_NO_ID = "None"
  val URL_HUB_PREFIX = "/wd/hub"
  val METHOD_GET = "GET"
  val METHOD_POST = "POST"
  val METHOD_DELETE = "DELETE"

  /* get start */
  object Status extends AppiumFunction {
    def apply(): IndigoMessage = buildGetRequestMessage("status")

    case class StatusModel(ready: Boolean, message: String)

    case class Response() extends AppiumResponse {
      override type T = StatusModel
      override var value: T = null
    }
  }

  object GetSessions extends AppiumFunction {
    def apply(): IndigoMessage = buildGetRequestMessage("sessions")

    case class SessionModel(sessionId: String, capabilities: java.util.Map[String, Object])

    case class Response() extends AppiumResponse {
      override type T = java.util.Collection[SessionModel]
      override var value: T = null
    }
  }

  object GetSessionDetails extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = buildGetRequestMessage(s"session/$sessionId")

    case class SessionDetailsModel(lastScrollData: java.util.Map[String, Int])

    case class Response() extends AppiumResponse {
      override type T = java.util.Collection[SessionDetailsModel]
      override var value: T = null
    }
  }

  object CaptureScreenshot extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = buildGetRequestMessage(s"session/$sessionId/screenshot")

    type Response = StringAppiumResponse // Base64-encoded screenshot string
  }

  object GetOrientation extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage =
      buildGetRequestMessage(s"session/$sessionId/orientation")

    type Response = StringAppiumResponse // 'PORTRAIT' or 'LANDSCAPE'
  }

  object GetRotation extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage =
      buildGetRequestMessage(s"session/$sessionId/rotation")

    case class RotationModel(x: Int, y: Int, z: Int)

    case class Response() extends AppiumResponse {
      override type T = RotationModel
      override var value: T = null
    }
  }

  object GetText extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage =
      buildGetRequestMessage(s"session/$sessionId/element/$id/text")

    type Response = StringAppiumResponse
  }

  object GetElementAttribute extends AppiumFunction {
    def apply(sessionId: String, id: String, name: String): IndigoMessage =
      buildGetRequestMessage(s"session/$sessionId/element/$id/attribute/$name")

    type Response = StringAppiumResponse
  }

  object GetRect extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage =
      buildGetRequestMessage(s"session/$sessionId/element/$id/rect")

    case class ElementRectModel(x: Int, y: Int, width: Int, height: Int)

    case class Response() extends AppiumResponse {
      override type T = ElementRectModel
      override var value: T = null
    }
  }

  object GetSize extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage =
      buildGetRequestMessage(s"session/$sessionId/element/$id/size")

    type Response = SizeAppiumResponse
  }

  object GetName extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage =
      buildGetRequestMessage(s"session/$sessionId/element/$id/name")

    type Response = StringAppiumResponse // ContentDesc
  }

  // JSONWP endpoint
  object GetElementScreenshot extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/screenshot/$id")
    }

    def w3c(sessionId: String, id: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/element/$id/screenshot")
    }

    type Response = StringAppiumResponse // ContentDesc
  }

  object Location extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/element/$id/location")
    }

    case class LocationModel(x: Int, y: Int)

    case class Response() extends AppiumResponse {
      override type T = LocationModel
      override var value: T = null
    }
  }

  object GetDeviceSize extends AppiumFunction {
    def apply(sessionId: String, windowHandle: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/window/$windowHandle/size")
    }

    type Response = SizeAppiumResponse
  }

  object Source extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/source")
    }

    type Response = StringAppiumResponse
  }

  object GetSystemBars extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/appium/device/system_bars")
    }

    case class SystemBarsModel(statusBar: Int)

    case class Response() extends AppiumResponse {
      override type T = SystemBarsModel
      override var value: T = null
    }
  }

  object GetBatteryInfo extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/appium/device/battery_info")
    }

    case class BatteryStatusModel(level: Double, status: Int)

    case class Response() extends AppiumResponse {
      override type T = BatteryStatusModel
      override var value: T = null
    }
  }

  object GetSettings extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/appium/settings")
    }

    type Response = MapAppiumResponse
  }

  object GetDevicePixelRatio extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/appium/device/pixel_ratio")
    }

    case class Response() extends AppiumResponse {
      override type T = Float
      override var value: T = 0f
    }
  }

  object FirstVisibleView extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/appium/element/$id/first_visible")
    }

    type Response = MapAppiumResponse
  }

  object GetAlertText extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/alert/text")
    }

    type Response = StringAppiumResponse
  }

  object GetDeviceInfo extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildGetRequestMessage(s"session/$sessionId/appium/device/info")
    }

    case class NetworkCapabilitiesModel(
                                         transportTypes: String,
                                         networkCapabilities: String,
                                         linkUpstreamBandwidthKbps: Int,
                                         linkDownBandwidthKbps: Int,
                                         signalStrength: Int,
                                         SSID: String,
                                       )

    case class NetworkInfoModel(
                                 `type`: Int,
                                 typeName: String,
                                 subtype: Int,
                                 subtypeName: String,
                                 isConnected: Boolean,
                                 detailedState: String,
                                 state: String,
                                 extraInfo: String,
                                 isAvailable: Boolean,
                                 isFailover: Boolean,
                                 isRoaming: Boolean,
                                 capabilities: NetworkCapabilitiesModel,
                               )

    case class BluetoothInfoModel(state: String)

    case class DeviceInfoModel(
                                androidId: String,
                                manufacturer: String,
                                model: String,
                                brand: String,
                                apiVersion: String,
                                platformVersion: String,
                                carrierName: String,
                                realDisplaySize: String,
                                displayDensity: Int,
                                networks: java.util.Collection[NetworkInfoModel],
                                locale: String,
                                timeZone: String,
                                bluetooth: BluetoothInfoModel,
                              )

    case class Response() extends AppiumResponse {
      override type T = DeviceInfoModel
      override var value: T = null
    }
  }
  /* get end */

  /* delete start */
  object DeleteSession extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildDeleteRequestMessage(s"session/$sessionId")
    }

    type Response = NullValueAppiumResponse
  }
  /* delete end */

  /* post start */
  object NewSession extends AppiumFunction {
    def apply(capabilities: java.util.Map[String, Object]): IndigoMessage = {
      buildPostRequestMessage("session", SessionModel(null, capabilities))
    }

    case class SessionModel(sessionId: String, capabilities: java.util.Map[String, Object])

    case class Response() extends AppiumResponse {
      override type T = SessionModel
      override var value: T = null
    }
  }

  object FindElement extends AppiumFunction {
    def apply(sessionId: String, body: FindElementModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/element", body)
    }

    type Response = MapAppiumResponse
  }

  object FindElements extends AppiumFunction {
    def apply(sessionId: String, body: FindElementModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/elements", body)
    }

    type Response = SeqMapAppiumResponse
  }

  object Click extends AppiumFunction {
    def apply(sessionId: String, id: String): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/element/$id/click")
    }

    type Response = NullValueAppiumResponse
  }

  object Tap extends AppiumFunction {
    def apply(sessionId: String, body: TapModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/tap", body)
    }

    case class TapModel(x: Double, y: Double) extends ElementModel

    type Response = NullValueAppiumResponse
  }

  object SetOrientation extends AppiumFunction {
    def apply(sessionId: String, orientation: String): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/orientation", OrientationModel(orientation))
    }

    case class OrientationModel(@Required orientation: String)

    type Response = StringAppiumResponse
  }

  object SetRotation extends AppiumFunction {
    def apply(sessionId: String, body: RotationModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/rotation", body)
    }

    case class RotationModel(x: Int, y: Int, @Required z: Int)

    type Response = StringAppiumResponse
  }

  object PressBack extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/back")
    }

    type Response = NullValueAppiumResponse
  }

  object SendKeysToElement extends AppiumFunction {
    def apply(sessionId: String, body: SendKeysModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/keys", body) // current focused
    }

    def apply(sessionId: String, id: String, body: SendKeysModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/element/$id/value", body)
    }

    case class SendKeysModel(replace: Boolean, @Required text: String)

    type Response = NullValueAppiumResponse
  }

  object Swipe extends AppiumFunction {
    def apply(sessionId: String, body: SwipeModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/perform", body)
    }

    case class SwipeModel(
                           elementId: String,
                           @Required startX: Double,
                           @Required startY: Double,
                           @Required endX: Double,
                           @Required endY: Double,
                           @Required steps: Int
                         )

    type Response = NullValueAppiumResponse
  }

  object TouchLongClick extends AppiumFunction {
    def apply(sessionId: String, params: TouchEventParams): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/longclick", TouchEventModel(params))
    }

    type Response = NullValueAppiumResponse
  }

  object OpenNotification extends AppiumFunction {
    def apply(sessionId: String): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/device/open_notifications")
    }

    type Response = NullValueAppiumResponse
  }

  object PressKeyCode extends AppiumFunction {
    def apply(sessionId: String, body: KeyCodeModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/device/press_keycode", body)
    }

    type Response = NullValueAppiumResponse
  }

  object LongPressKeyCode extends AppiumFunction {
    def apply(sessionId: String, body: KeyCodeModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/device/long_press_keycode", body)
    }

    type Response = NullValueAppiumResponse
  }

  object Drag extends AppiumFunction {
    def apply(sessionId: String, body: DragModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/drag", body)
    }

    case class DragModel(
                          elementId: String, destElId: String,
                          startX: Double, startY: Double,
                          endX: Double, endY: Double,
                          @Required steps: Int
                        )

    type Response = NullValueAppiumResponse
  }

  object Flick extends AppiumFunction {
    def apply(sessionId: String, id: String, body: FlickByOffsetModel): IndigoMessage = {
      body.jwpElementId = id
      buildPostRequestMessage(s"session/$sessionId/touch/flick", body)
    }

    def apply(sessionId: String, body: FlickBySpeedModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/flick", body)
    }

    case class FlickByOffsetModel(
                                   @Required xoffset: Int, @Required yoffset: Int,
                                   @Required speed: Int
                                 ) extends ElementModel

    case class FlickBySpeedModel(@Required xspeed: Int, @Required yspeed: Int)

    type Response = NullValueAppiumResponse
  }

  object ScrollTo extends AppiumFunction {
    def apply(sessionId: String, body: ScrollToModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/scroll", body)
    }

    case class ScrollParams(@Required strategy: String, @Required selector: String, maxSwipes: Int)

    case class ScrollToModel(origin: ElementModel, @Required params: ScrollParams)

    type Response = NullValueAppiumResponse
  }

  object MultiPointerGesture extends AppiumFunction {
    def apply(sessionId: String, body: TouchActionsModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/multi/perform", body)
    }

    case class TouchLocationModel(@Required x: Double, @Required y: Double)

    case class TouchGestureModel(@Required touch: TouchLocationModel, @Required time: Double)

    case class TouchActionsModel(@Required actions: java.util.Collection[TouchGestureModel])

    type Response = NullValueAppiumResponse
  }

  object W3CActions extends AppiumFunction {
    def apply(sessionId: String, body: W3CActionsModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/actions", body)
    }

    case class W3CItemParametersModel(pointerType: String)

    case class W3CGestureModel(
                                @Required `type`: String, duration: Long, origin: Object,
                                x: Double, y: Double, button: Int, value: String,
                                size: Double, pressure: Double
                              )

    case class W3CItemModel(
                             @Required `type`: String, @Required id: String,
                             parameters: W3CItemParametersModel, actions: java.util.Collection[W3CGestureModel],
                           )

    case class W3CActionsModel(@Required actions: java.util.Collection[W3CItemModel])

    type Response = NullValueAppiumResponse
  }

  object TouchDown extends AppiumFunction {
    def apply(sessionId: String, params: TouchEventParams): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/down", TouchEventModel(params))
    }

    type Response = NullValueAppiumResponse
  }

  object TouchUp extends AppiumFunction {
    def apply(sessionId: String, params: TouchEventParams): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/up", TouchEventModel(params))
    }

    type Response = NullValueAppiumResponse
  }

  object TouchMove extends AppiumFunction {
    def apply(sessionId: String, params: TouchEventParams): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/touch/move", TouchEventModel(params))
    }

    type Response = NullValueAppiumResponse
  }

  object UpdateSettings extends AppiumFunction {
    def apply(sessionId: String, settings: java.util.Map[String, Object]): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/settings", SettingsModel(settings))
    }

    case class SettingsModel(@Required settings: java.util.Map[String, Object])

    type Response = NullValueAppiumResponse
  }

  object NetworkConnection extends AppiumFunction {
    def apply(sessionId: String, body: NetworkConnectionModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/network_connection", body)
    }

    case class NetworkConnectionModel(`type`: Int)

    type Response = IntAppiumResponse
  }

  object ScrollToElement extends AppiumFunction {
    def apply(sessionId: String, from: String, to: String): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/element/$from/scroll_to/$to")
    }

    type Response = NullValueAppiumResponse
  }

  object GetClipboard extends AppiumFunction {
    def apply(sessionId: String, body: GetClipboardModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/device/get_clipboard", body)
    }

    case class GetClipboardModel(contentType: String)

    type Response = StringAppiumResponse
  }

  object SetClipboard extends AppiumFunction {
    def apply(sessionId: String, body: SetClipboardModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/device/set_clipboard", body)
    }

    case class SetClipboardModel(@Required content: String, contentType: String, label: String)

    type Response = NullValueAppiumResponse
  }

  object AcceptAlert extends AppiumFunction {
    def apply(sessionId: String, buttonLabel: String): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/alert/accept", AlertModel(buttonLabel))
    }

    type Response = NullValueAppiumResponse
  }

  object DismissAlert extends AppiumFunction {
    def apply(sessionId: String, buttonLabel: String): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/alert/dismiss", AlertModel(buttonLabel))
    }

    type Response = NullValueAppiumResponse
  }

  object GesturesDrag extends AppiumFunction {
    def apply(sessionId: String, body: DragModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/drag", body)
    }

    case class DragModel(origin: ElementModel, start: PointModel, @Required end: PointModel, speed: Int)

    type Response = NullValueAppiumResponse
  }

  object GesturesFling extends AppiumFunction {
    def apply(sessionId: String, body: FlingModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/fling", body)
    }

    case class FlingModel(origin: ElementModel, area: RectModel, @Required direction: String, speed: Int)

    type Response = NullValueAppiumResponse
  }

  object GesturesLongClick extends AppiumFunction {
    def apply(sessionId: String, body: LongClickModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/long_click", body)
    }

    case class LongClickModel(origin: ElementModel, offset: PointModel, direction: Double)

    type Response = NullValueAppiumResponse
  }

  object GesturesDoubleClick extends AppiumFunction {
    def apply(sessionId: String, body: DoubleClickModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/double_click", body)
    }

    case class DoubleClickModel(origin: ElementModel, offset: PointModel)

    type Response = NullValueAppiumResponse
  }

  object GesturesPinchClose extends AppiumFunction {
    def apply(sessionId: String, body: PinchModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/pinch_close", body)
    }

    type Response = NullValueAppiumResponse
  }

  object GesturesPinchOpen extends AppiumFunction {
    def apply(sessionId: String, body: PinchModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/pinch_open", body)
    }

    type Response = NullValueAppiumResponse
  }

  object GesturesScroll extends AppiumFunction {
    def apply(sessionId: String, body: ScrollModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/scroll", body)
    }

    case class ScrollModel(
                            origin: ElementModel, area: RectModel,
                            @Required direction: String, @Required percent: Float, speed: Int
                          )

    type Response = BooleanAppiumResponse
  }

  object GesturesSwipe extends AppiumFunction {
    def apply(sessionId: String, body: SwipeModel): IndigoMessage = {
      buildPostRequestMessage(s"session/$sessionId/appium/gestures/swipe", body)
    }

    case class SwipeModel(
                           origin: ElementModel, area: RectModel,
                           @Required direction: String, @Required percent: Float, speed: Int
                         )

    type Response = NullValueAppiumResponse
  }
  /* post end */

  case class PinchModel(origin: ElementModel, area: RectModel, @Required percent: Float, speed: Int)

  case class RectModel(@Required top: Double, @Required left: Double, @Required width: Double, @Required height: Double)

  case class PointModel(@Required x: Double, @Required y: Double)

  case class AlertModel(buttonLabel: String)

  case class TouchEventParams(x: Double, y: Double, duration: Double) extends ElementModel

  case class TouchEventModel(@Required params: TouchEventParams)

  case class KeyCodeModel(@Required keycode: Int, metastate: Int, flags: Int)

  class ElementModel { // only need set one filed
    @JsonProperty("ELEMENT")
    @JsonAlias(Array("element")) var jwpElementId: String = null
    @JsonProperty("element-6066-11e4-a52e-4f735466cecf") var w3cElementId: String = null
  }

  object ElementModel {
    def apply(id: String): ElementModel = {
      val model = new ElementModel()
      model.jwpElementId = id
      model
    }
  }

  case class FindElementModel(@Required strategy: String, @Required selector: String, context: String)

  case class MapAppiumResponse() extends AppiumResponse {
    override type T = java.util.Map[String, Object]
    override var value: T = null
  }

  case class SeqMapAppiumResponse() extends AppiumResponse {
    override type T = java.util.Collection[java.util.Map[String, Object]]
    override var value: T = null
  }

  case class SizeModel(width: Int, height: Int)

  case class SizeAppiumResponse() extends AppiumResponse {
    override type T = SizeModel
    override var value: T = null
  }

  type NullValueAppiumResponse = StringAppiumResponse // value is null

  case class BooleanAppiumResponse() extends AppiumResponse {
    override type T = Boolean
    override var value: T = false
  }

  case class IntAppiumResponse() extends AppiumResponse {
    override type T = Integer
    override var value: T = null
  }

  case class StringAppiumResponse() extends AppiumResponse {
    override type T = String
    override var value: T = null
  }

  implicit class IndigoMessageFunctions(msg: IndigoMessage) {
    def send()(implicit func: IndigoMessage => IndigoMessage): IndigoMessage = {
      func(msg)
    }

    def resBodyAs[T >: Null <: AnyRef](c: Class[T]): T = {
      if (msg.res != null && msg.res.content != null) {
        if (msg.res.status == 200) {
          JsonUtils.parse(msg.res.content, c)
        } else { // exception
          logger.debug(msg.res.content)
          val model = JsonUtils.parse(msg.res.content, classOf[AppiumExceptionModel])
          throw new RuntimeException(s"${model.error}: ${model.message}")
        }
      } else {
        null
      }
    }

    def resBodyAsMap: java.util.Map[Object, Object] = {
      resBodyAs(classOf[java.util.Map[Object, Object]])
    }
  }

  case class AppiumExceptionModel(error: String, message: String, stacktrace: String)

  trait AppiumFunction {
    type Response <: AppiumResponse
  }

  abstract class AppiumResponse {
    type T
    var value: T
    var sessionId: String = null

    def asMap: java.util.Map[Object, Object] = {
      if (value != null) {
        JsonUtils.mapper.convertValue(value, classOf[java.util.HashMap[Object, Object]])
      } else {
        null
      }
    }
  }

  def buildGetRequestMessage(path: String): IndigoMessage = {
    val message = new IndigoMessage(IndigoMessageType.WD_REQ)
    val req = new HttpRequest()
    req.method = METHOD_GET
    req.uri = s"$URL_HUB_PREFIX/$path"
    message.req = req
    message
  }

  def buildDeleteRequestMessage(path: String): IndigoMessage = {
    val message = new IndigoMessage(IndigoMessageType.WD_REQ)
    val req = new HttpRequest()
    req.method = METHOD_DELETE
    req.uri = s"$URL_HUB_PREFIX/$path"
    message.req = req
    message
  }

  def buildPostRequestMessage(path: String, body: Object = null): IndigoMessage = {
    val message = new IndigoMessage(IndigoMessageType.WD_REQ)
    val req = new HttpRequest()
    req.method = METHOD_POST
    req.uri = s"$URL_HUB_PREFIX/$path"
    if (body != null) {
      req.body = if (body.isInstanceOf[String]) body.asInstanceOf[String] else JsonUtils.stringify(body)
    }
    message.req = req
    message
  }

}
