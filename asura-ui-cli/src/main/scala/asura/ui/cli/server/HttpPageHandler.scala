package asura.ui.cli.server

import java.nio.charset.StandardCharsets

import io.netty.karate.buffer.{ByteBufUtil, Unpooled}
import io.netty.karate.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.karate.handler.codec.http.HttpHeaderNames._
import io.netty.karate.handler.codec.http.HttpResponseStatus._
import io.netty.karate.handler.codec.http._

class HttpPageHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  override def channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    if (!req.decoderResult().isSuccess) {
      sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.protocolVersion(), BAD_REQUEST, ctx.alloc().buffer(0)))
    } else {
      val content = Unpooled.copiedBuffer("ok", StandardCharsets.UTF_8)
      val res = new DefaultFullHttpResponse(req.protocolVersion(), OK, content)
      res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8")
      HttpUtil.setContentLength(res, content.readableBytes())
      sendHttpResponse(ctx, req, res)
    }
  }

  def sendHttpResponse(ctx: ChannelHandlerContext, req: FullHttpRequest, res: FullHttpResponse): Unit = {
    val status = res.status()
    if (status.code() != 200) {
      ByteBufUtil.writeUtf8(res.content(), status.toString)
      HttpUtil.setContentLength(res, res.content().readableBytes())
    }
    ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE)
  }

}
