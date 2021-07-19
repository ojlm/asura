package asura.ui.cli.server

import asura.ui.cli.server.ProxyServerConnectHandler.logger
import com.typesafe.scalalogging.Logger
import karate.io.netty.bootstrap.Bootstrap
import karate.io.netty.channel._
import karate.io.netty.channel.socket.nio.NioSocketChannel
import karate.io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import karate.io.netty.handler.codec.http.websocketx.extensions.compression.{WebSocketClientCompressionHandler, WebSocketServerCompressionHandler}
import karate.io.netty.handler.codec.http.{HttpClientCodec, HttpObjectAggregator, HttpServerCodec}
import karate.io.netty.handler.logging.{LogLevel, LoggingHandler}
import karate.io.netty.handler.stream.ChunkedWriteHandler
import karate.io.netty.util.concurrent.Future

class ProxyServerConnectHandler(
                                 remoteHost: String,
                                 remotePort: Int,
                                 isHttp: Boolean,
                                 enableHttpKeepAlive: Boolean,
                                 wsConfig: WebSocketProxyConfig,
                               ) extends ChannelInboundHandlerAdapter {

  val bootstrap = new Bootstrap()

  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    val promise = ctx.executor().newPromise[Channel]()
    promise.addListener((future: Future[Channel]) => {
      val outboundChannel = future.getNow
      if (future.isSuccess) {
        if (isHttp) {
          val outPipeline = outboundChannel.pipeline()
          outPipeline.addLast(new HttpClientCodec())
          outPipeline.addLast(new HttpObjectAggregator(104857600))
          if (wsConfig == null) {
            outPipeline.addLast(new ChunkedWriteHandler())
            outPipeline.addLast(new HttpRelayHandler(ctx.channel(), enableHttpKeepAlive))
          } else {
            outPipeline.addLast(WebSocketClientCompressionHandler.INSTANCE)
            outPipeline.addLast(wsConfig.clientHandler(ctx.channel()))
            // fire the client handshake
            outPipeline.fireChannelActive()
          }
          val inPipeline = ctx.pipeline()
          inPipeline.addLast(new HttpServerCodec())
          inPipeline.addLast(new HttpObjectAggregator(104857600))
          if (wsConfig == null) {
            inPipeline.addLast(new ChunkedWriteHandler())
            inPipeline.addLast(new HttpRelayHandler(outboundChannel, enableHttpKeepAlive))
          } else {
            inPipeline.addLast(new WebSocketServerCompressionHandler())
            inPipeline.addLast(new WebSocketServerProtocolHandler(wsConfig.serverConfig))
            inPipeline.addLast(wsConfig.serverHandler(outboundChannel))
          }
        } else {
          outboundChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO))
          outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()))
          ctx.pipeline().addLast(new RelayHandler(outboundChannel))
        }
        ctx.pipeline().remove(this)
        ctx.fireChannelRead(msg)
      } else {
        NettyUtils.flushAndClose(ctx.channel())
      }
    })
    val inboundChannel = ctx.channel()
    bootstrap.group(inboundChannel.eventLoop())
      .channel(classOf[NioSocketChannel])
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Int.box(10000))
      .option(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
      .handler(new DirectClientHandler(promise))
    bootstrap.connect(remoteHost, remotePort).addListener((future: ChannelFuture) => {
      if (future.isSuccess) {
        logger.debug(s"proxy connect to $remoteHost:$remotePort success")
      } else {
        logger.debug(s"proxy connect to $remoteHost:$remotePort fail")
        NettyUtils.flushAndClose(ctx.channel())
      }
    })
  }

}

object ProxyServerConnectHandler {

  val logger = Logger("ProxyServerConnectHandler")

}
