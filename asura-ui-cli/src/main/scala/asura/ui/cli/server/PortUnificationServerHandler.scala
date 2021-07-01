package asura.ui.cli.server

import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util

import asura.ui.cli.server.PortUnificationServerHandler._
import com.typesafe.scalalogging.Logger
import karate.io.netty.buffer.ByteBuf
import karate.io.netty.channel.{ChannelHandlerContext, ChannelPipeline}
import karate.io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import karate.io.netty.handler.codec.http.websocketx.{WebSocketDecoderConfig, WebSocketServerProtocolConfig, WebSocketServerProtocolHandler}
import karate.io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import karate.io.netty.handler.codec.{ByteToMessageDecoder, LengthFieldBasedFrameDecoder}
import karate.io.netty.handler.logging.{LogLevel, LoggingHandler}
import karate.io.netty.handler.ssl.{SslContext, SslHandler}
import karate.io.netty.handler.stream.ChunkedWriteHandler

class PortUnificationServerHandler(
                                    sslCtx: SslContext,
                                    config: ServerProxyConfig,
                                    detectSsl: Boolean = true,
                                  ) extends ByteToMessageDecoder {

  import PortUnificationServerHandler.logger

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
            if (config.isChrome(uri)) { // chrome
              if (uri.startsWith("/devtools/page/")) { // websocket
                val pageId = uri.substring("/devtools/page/".size)
                switchToTcpProxy(ctx, "127.0.0.1", config.portSelector.getPort(pageId))
              } else {
                switchToHttpProxy(ctx, "127.0.0.1", config.portSelector.getPort(null))
              }
            } else if (config.isWebsockify(uri)) { // vnc server
              switchToHttpProxy(ctx, "127.0.0.1", config.localWebsockifyPort)
            } else {
              switchToLocalHttp(ctx)
            }
          }
        } else {
          if (config.enableScrcpy) {
            if (in.readableBytes() >= SCRCPY_DEVICE_HEADER_LENGTH) {
              switchToAppProxy(ctx, in)
            }
          } else {
            in.clear()
            ctx.close()
          }
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
    pipeline.addLast(new PortUnificationServerHandler(sslCtx, config, false))
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
    p.fireChannelActive()
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

  def switchToAppProxy(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val p = ctx.pipeline()
    val zeroIdx = msg.indexOf(msg.readerIndex(), SCRCPY_DEVICE_NAME_FIELD_LENGTH, 0)
    val nameLength = if (zeroIdx > SCRCPY_DEVICE_NAME_FIELD_LENGTH) SCRCPY_DEVICE_NAME_FIELD_LENGTH else zeroIdx - msg.readerIndex()
    // "${prefix}:${serial}". prefix: 'I'(indigo), 'A'(appium), 'V'(scrcpy video), 'C'(scrcpy control)
    val device = msg.toString(msg.readerIndex(), nameLength, StandardCharsets.UTF_8)
    msg.skipBytes(SCRCPY_DEVICE_NAME_FIELD_LENGTH)
    val width = msg.readShort()
    val height = msg.readShort()
    val serial = device.substring(2)
    if (device.startsWith("I:")) {
      logger.info(s"indigo controller connected: $serial ${width}x${height}")
      addIndigoHandler(p, serial, true)
    } else if (device.startsWith("A:")) {
      logger.info(s"indigo appium connected: $serial ${width}x${height}")
      addIndigoHandler(p, serial, false)
    } else if (device.startsWith("V:")) {
      logger.info(s"scrcpy video connected: $serial ${width}x${height}")
      p.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE, 8, 4, 0, 0, true))
      if (config.dumpScrcpy) {
        p.addLast(new LoggingHandler(LogLevel.INFO))
      }
      p.addLast(new ScrcpyVideoHandler(serial, width, height))
    } else if (device.startsWith("C:")) {
      logger.info(s"scrcpy control connected: $serial ${width}x${height}")
      if (config.dumpScrcpy) {
        p.addLast(new LoggingHandler(LogLevel.INFO))
      }
      p.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE, 1, 4, 0, 0, true))
      p.addLast(new ScrcpyMessageCodec())
      p.addLast(new ScrcpyMessageHandler(serial))
    } else {
      p.addLast(new LoggingHandler(LogLevel.INFO))
    }
    p.fireChannelActive()
    p.remove(this)
  }

  def addIndigoHandler(p: ChannelPipeline, device: String, isController: Boolean): Unit = {
    p.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE, 0, 4, 0, 4, true))
    p.addLast(new IndigoMessageCodec())
    p.addLast(new IndigoMessageHandler(device, isController))
  }

}

object PortUnificationServerHandler {

  val logger = Logger(getClass)
  val SCRCPY_DEVICE_NAME_FIELD_LENGTH = 64
  val SCRCPY_DEVICE_SCREEN_FIELD_LENGTH = 4
  val SCRCPY_DEVICE_HEADER_LENGTH = SCRCPY_DEVICE_NAME_FIELD_LENGTH + SCRCPY_DEVICE_SCREEN_FIELD_LENGTH

}
