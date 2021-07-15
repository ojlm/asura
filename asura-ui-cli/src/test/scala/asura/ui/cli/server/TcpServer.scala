package asura.ui.cli.server

import java.nio.ByteOrder

import asura.ui.cli.push.TcpPushClient.{PushDataMessageCodec, PushDataMessageHandler}
import com.typesafe.scalalogging.Logger
import karate.io.netty.bootstrap.ServerBootstrap
import karate.io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerDomainSocketChannel}
import karate.io.netty.channel.nio.NioEventLoopGroup
import karate.io.netty.channel.socket.SocketChannel
import karate.io.netty.channel.socket.nio.NioServerSocketChannel
import karate.io.netty.channel.unix.{DomainSocketAddress, UnixChannel}
import karate.io.netty.channel.{ChannelHandlerContext, ChannelInitializer, SimpleChannelInboundHandler}
import karate.io.netty.handler.codec.LengthFieldBasedFrameDecoder
import karate.io.netty.handler.codec.http.websocketx._
import karate.io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import karate.io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import karate.io.netty.handler.stream.ChunkedWriteHandler

object TcpServer {

  val logger = Logger("TcpServer")

  def main(args: Array[String]): Unit = {
    ws()
  }

  def ws(): Unit = {
    val bootstrap = new ServerBootstrap()
    val wsServerConfig = WebSocketServerProtocolConfig.newBuilder()
      .websocketPath("/ws")
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
    bootstrap.group(new NioEventLoopGroup(2))
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast(new HttpServerCodec())
          pipeline.addLast(new HttpObjectAggregator(104857600))
          pipeline.addLast(new ChunkedWriteHandler())
          pipeline.addLast(new WebSocketServerCompressionHandler())
          pipeline.addLast(new WebSocketServerProtocolHandler(wsServerConfig))
          pipeline.addLast(new DebugSocketFrameHandler())
        }
      })
    bootstrap.bind("127.0.0.1", 9090).sync()
  }

  def tcp(): Unit = {
    val bootstrap = new ServerBootstrap()
    bootstrap.group(new NioEventLoopGroup(2))
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE, 0, 4, 0, 4, true))
          pipeline.addLast(new PushDataMessageCodec())
          pipeline.addLast(new PushDataMessageHandler())
        }
      })
    bootstrap.bind("127.0.0.1", 9090).sync()
  }

  def unix(): Unit = {
    val bootstrap = new ServerBootstrap()
    bootstrap.group(new EpollEventLoopGroup(2))
      .channel(classOf[EpollServerDomainSocketChannel])
      .childHandler(new ChannelInitializer[UnixChannel] {
        override def initChannel(ch: UnixChannel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE, 0, 4, 0, 4, true))
          pipeline.addLast(new PushDataMessageCodec())
          pipeline.addLast(new PushDataMessageHandler())
        }
      })
    bootstrap.bind(new DomainSocketAddress("/tmp/indigo.sock")).sync()
  }

  class DebugSocketFrameHandler extends SimpleChannelInboundHandler[WebSocketFrame] {
    override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame): Unit = {
      if (frame.isInstanceOf[TextWebSocketFrame]) {
        val msg = frame.asInstanceOf[TextWebSocketFrame]
        logger.info(s"==> ${msg.text()}")
      } else {
        val message = "Unsupported frame type: " + frame.getClass.getName
        throw new UnsupportedOperationException(message)
      }
    }
  }
}
