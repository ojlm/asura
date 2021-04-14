package asura.ui.cli.server

import java.nio.charset.StandardCharsets
import java.util

import karate.io.netty.buffer.ByteBuf
import karate.io.netty.channel.ChannelHandlerContext
import karate.io.netty.handler.codec.ByteToMessageDecoder
import karate.io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import karate.io.netty.handler.codec.http.websocketx.{WebSocketDecoderConfig, WebSocketServerProtocolConfig, WebSocketServerProtocolHandler}
import karate.io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import karate.io.netty.handler.ssl.{SslContext, SslHandler}
import karate.io.netty.handler.stream.ChunkedWriteHandler

class PortUnificationServerHandler(
                                    sslCtx: SslContext,
                                    proxyConfig: ServerProxyConfig,
                                    detectSsl: Boolean = true,
                                  ) extends ByteToMessageDecoder {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() >= 5) {
      if (isSsl(in)) {
        enableSsl(ctx)
      } else {
        val magic1 = in.getUnsignedByte(in.readerIndex())
        val magic2 = in.getUnsignedByte(in.readerIndex() + 1)
        if (isHttp(magic1, magic2)) {
          val tuple = findUrl(in)
          if (tuple._1 > -1 && tuple._2 > -1) {
            val uri = in.getCharSequence(tuple._1 + 1, tuple._2 - tuple._1 - 1, StandardCharsets.UTF_8).asInstanceOf[String]
            if (proxyConfig.isChrome(uri)) { // chrome
              if (uri.startsWith("/devtools/page/")) { // websocket
                switchToTcpProxy(ctx, "127.0.0.1", proxyConfig.localChromePort)
              } else {
                switchToHttpProxy(ctx, "127.0.0.1", proxyConfig.localChromePort)
              }
            } else if (proxyConfig.isWebsockify(uri)) { // vnc server
              switchToHttpProxy(ctx, "127.0.0.1", proxyConfig.localWebsockifyPort)
            } else {
              switchToLocalHttp(ctx)
            }
          }
        } else {
          in.skipBytes(in.readableBytes)
          ctx.close()
        }
      }
    }
  }

  def isSsl(buf: ByteBuf): Boolean = {
    if (detectSsl) {
      SslHandler.isEncrypted(buf)
    } else {
      false
    }
  }

  def isHttp(magic1: Int, magic2: Int): Boolean = {
    magic1 == 'G' && magic2 == 'E' || // GET
      magic1 == 'P' && magic2 == 'O' || // POST
      magic1 == 'P' && magic2 == 'U' || // PUT
      magic1 == 'H' && magic2 == 'E' || // HEAD
      magic1 == 'O' && magic2 == 'P' || // OPTIONS
      magic1 == 'P' && magic2 == 'A' || // PATCH
      magic1 == 'D' && magic2 == 'E' || // DELETE
      magic1 == 'T' && magic2 == 'R' || // TRACE
      magic1 == 'C' && magic2 == 'O' // CONNECT
  }

  def findUrl(buf: ByteBuf): (Int, Int) = {
    val firstSpace = buf.indexOf(buf.readerIndex(), buf.writerIndex(), ' ')
    var secondSpace = -1
    if (firstSpace > -1) {
      secondSpace = buf.indexOf(firstSpace + 1, buf.writerIndex(), ' ')
    }
    (firstSpace, secondSpace)
  }

  def enableSsl(ctx: ChannelHandlerContext): Unit = {
    val pipeline = ctx.pipeline()
    pipeline.addLast(sslCtx.newHandler(ctx.alloc()))
    pipeline.addLast(new PortUnificationServerHandler(sslCtx, proxyConfig, false))
    pipeline.remove(this)
  }

  private val wsServerConfig = WebSocketServerProtocolConfig.newBuilder()
    .websocketPath("/api/ws")
    .subprotocols(null)
    .checkStartsWith(true)
    .handshakeTimeoutMillis(10000L)
    .dropPongFrames(true)
    .handleCloseFrames(true)
    .decoderConfig(
      WebSocketDecoderConfig.newBuilder()
        .maxFramePayloadLength(104857600) // 100M
        .allowMaskMismatch(false)
        .allowExtensions(true)
        .build()
    ).build()

  def switchToLocalHttp(ctx: ChannelHandlerContext): Unit = {
    val p = ctx.pipeline()
    p.addLast(new HttpServerCodec())
    p.addLast(new HttpObjectAggregator(104857600)) // 100M
    p.addLast(new ChunkedWriteHandler())
    p.addLast(new WebSocketServerCompressionHandler())
    p.addLast(new WebSocketServerProtocolHandler(wsServerConfig))
    p.addLast(new HttpPageHandler(false))
    p.addLast(new WebSocketFrameHandler())
    p.remove(this)
  }

  def switchToHttpProxy(ctx: ChannelHandlerContext, remoteHost: String, remotePort: Int): Unit = {
    val p = ctx.pipeline()
    p.addLast(new ProxyServerConnectHandler(remoteHost, remotePort, true, false))
    p.remove(this)
  }

  def switchToTcpProxy(ctx: ChannelHandlerContext, remoteHost: String, remotePort: Int): Unit = {
    val p = ctx.pipeline()
    p.addLast(new ProxyServerConnectHandler(remoteHost, remotePort, false, false))
    p.remove(this)
  }

}
