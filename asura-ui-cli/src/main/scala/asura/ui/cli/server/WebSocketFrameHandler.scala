package asura.ui.cli.server

import java.util.concurrent.ConcurrentHashMap

import asura.common.util.{JsonUtils, StringUtils}
import asura.ui.cli.server.WebSocketFrameHandler.{DEVICE_CONNECT_SCRCPY, DEVICE_CONNECT_WD}
import asura.ui.hub.Hubs.{DeviceWdHub, IndigoAppiumHub, RawH264StreamHub}
import asura.ui.hub.{RawH264Packet, Sink}
import asura.ui.message.IndigoMessage
import karate.io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import karate.io.netty.handler.codec.http.QueryStringDecoder
import karate.io.netty.handler.codec.http.websocketx.{BinaryWebSocketFrame, TextWebSocketFrame, WebSocketFrame, WebSocketServerProtocolHandler}

class WebSocketFrameHandler extends SimpleChannelInboundHandler[WebSocketFrame] {

  val DEVICE_URL_PATH_PREFIX = "/api/ws/device/"
  var device: String = null
  var deviceConnectionType = -1
  var chn: Channel = null
  var rawH264PacketSink: Sink[RawH264Packet] = null
  var deviceWdSink: Sink[IndigoMessage] = null
  var deviceAppiumSinks: ConcurrentHashMap[Sink[IndigoMessage], Sink[IndigoMessage]] = null

  override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame): Unit = {
    if (frame.isInstanceOf[TextWebSocketFrame]) {
      if (StringUtils.isNotEmpty(device) && deviceConnectionType == DEVICE_CONNECT_WD) {
        val message = JsonUtils.parse(frame.asInstanceOf[TextWebSocketFrame].text(), classOf[IndigoMessage])
        IndigoAppiumHub.write(deviceAppiumSinks, message)
      } else {
        val txt = frame.asInstanceOf[TextWebSocketFrame].text()
        ctx.channel.writeAndFlush(new TextWebSocketFrame(txt))
      }
    } else {
      val message = "Unsupported frame type: " + frame.getClass.getName
      throw new UnsupportedOperationException(message)
    }
  }

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: Any): Unit = {
    if (evt.isInstanceOf[WebSocketServerProtocolHandler.HandshakeComplete]) {
      val handshake = evt.asInstanceOf[WebSocketServerProtocolHandler.HandshakeComplete]
      val uri = new QueryStringDecoder(handshake.requestUri())
      val path = uri.path()
      if (path.startsWith(DEVICE_URL_PATH_PREFIX)) {
        val paths = path.split("/")
        if (paths.length >= 6) {
          device = paths(4)
          paths(5) match {
            case "wd" => // WebDriver
              deviceConnectionType = DEVICE_CONNECT_WD
              deviceAppiumSinks = IndigoAppiumHub.getSinks(device)
              deviceWdSink = new ChannelSink[IndigoMessage](chn) {
                override def write(frame: IndigoMessage): Boolean = {
                  if (chn.isActive && chn.isWritable) {
                    chn.writeAndFlush(new TextWebSocketFrame(JsonUtils.stringify(frame)))
                    true
                  } else {
                    false
                  }
                }
              }
              DeviceWdHub.enter(device, deviceWdSink)
            case "scrcpy" => // Screen
              deviceConnectionType = DEVICE_CONNECT_SCRCPY
              rawH264PacketSink = new ChannelSink[RawH264Packet](chn) {
                override def write(frame: RawH264Packet): Boolean = {
                  if (chn.isActive && chn.isWritable) {
                    // TODO: need write SPS and PPS units first
                    // TODO: reduce heap bytes gc
                    val buf = chn.alloc().buffer(frame.buf.length).writeBytes(frame.buf)
                    chn.writeAndFlush(new BinaryWebSocketFrame(buf))
                    true
                  } else {
                    false
                  }
                }
              }
              RawH264StreamHub.enter(device, rawH264PacketSink)
            case _ =>
          }
        }
      }
    }
    super.userEventTriggered(ctx, evt)
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
    chn = ctx.channel()
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    super.channelInactive(ctx)
    if (StringUtils.isNotEmpty(device)) {
      if (rawH264PacketSink != null) {
        RawH264StreamHub.leave(device, rawH264PacketSink)
      }
      if (deviceWdSink != null) {
        DeviceWdHub.leave(device, deviceWdSink)
      }
    }
  }

}

object WebSocketFrameHandler {
  val DEVICE_CONNECT_WD = 0
  val DEVICE_CONNECT_SCRCPY = 1
}
