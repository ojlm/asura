package asura.ui.cli.server

import io.netty.karate.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.karate.handler.codec.http.websocketx.{TextWebSocketFrame, WebSocketFrame}

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
