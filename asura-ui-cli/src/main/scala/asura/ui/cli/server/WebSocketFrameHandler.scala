package asura.ui.cli.server

import asura.common.util.StringUtils
import asura.ui.cli.hub.Hubs.StreamHub
import asura.ui.cli.hub.{Sink, StreamFrame}
import karate.io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import karate.io.netty.handler.codec.http.QueryStringDecoder
import karate.io.netty.handler.codec.http.websocketx.{BinaryWebSocketFrame, TextWebSocketFrame, WebSocketFrame, WebSocketServerProtocolHandler}

class WebSocketFrameHandler extends SimpleChannelInboundHandler[WebSocketFrame] with Sink[StreamFrame] {

  val DEVICE_URL_PATH_PREFIX = "/api/ws/device/"
  var device: String = null
  var chn: Channel = null

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: Any): Unit = {
    if (evt.isInstanceOf[WebSocketServerProtocolHandler.HandshakeComplete]) {
      val handshake = evt.asInstanceOf[WebSocketServerProtocolHandler.HandshakeComplete]
      val uri = new QueryStringDecoder(handshake.requestUri())
      val path = uri.path()
      if (path.startsWith(DEVICE_URL_PATH_PREFIX)) {
        device = path.substring(DEVICE_URL_PATH_PREFIX.length)
        StreamHub.enter(device, this)
      }
    }
    super.userEventTriggered(ctx, evt)
  }

  override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame): Unit = {
    if (StringUtils.isNotEmpty(device)) {
      // TODO
    } else {
      if (frame.isInstanceOf[TextWebSocketFrame]) {
        val txt = frame.asInstanceOf[TextWebSocketFrame].text()
        ctx.channel.writeAndFlush(new TextWebSocketFrame(txt))
      } else {
        val message = "Unsupported frame type: " + frame.getClass.getName
        throw new UnsupportedOperationException(message)
      }
    }
  }

  override def write(frame: StreamFrame): Boolean = {
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

  override def close(): Unit = {
    if (chn.isActive) {
      chn.close()
    }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    super.channelActive(ctx)
    chn = ctx.channel()
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    super.channelInactive(ctx)
    if (StringUtils.isNotEmpty(device)) {
      StreamHub.leave(device, this)
    }
  }

}
