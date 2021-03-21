package asura.ui.cli.server

import karate.io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import karate.io.netty.handler.codec.http.websocketx.{TextWebSocketFrame, WebSocketFrame}

class WebSocketFrameHandler extends SimpleChannelInboundHandler[WebSocketFrame] {

  override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame): Unit = {
    // ping and pong frames already handled
    if (frame.isInstanceOf[TextWebSocketFrame]) {
      val txt = frame.asInstanceOf[TextWebSocketFrame].text()
      ctx.channel.writeAndFlush(new TextWebSocketFrame(txt))
    } else {
      val message = "Unsupported frame type: " + frame.getClass.getName
      throw new UnsupportedOperationException(message)
    }
  }

}
