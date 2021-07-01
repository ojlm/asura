package asura.ui.cli.push

import java.net.URI

import asura.common.util.JsonUtils
import asura.ui.cli.CliSystem
import com.typesafe.scalalogging.Logger
import karate.io.netty.bootstrap.Bootstrap
import karate.io.netty.buffer.Unpooled
import karate.io.netty.channel._
import karate.io.netty.channel.socket.SocketChannel
import karate.io.netty.channel.socket.nio.NioSocketChannel
import karate.io.netty.handler.codec.http.websocketx._
import karate.io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler
import karate.io.netty.handler.codec.http.{DefaultHttpHeaders, FullHttpResponse, HttpClientCodec, HttpObjectAggregator}
import karate.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import karate.io.netty.handler.ssl.{SslContext, SslContextBuilder}

case class WebsocketPushClient(options: PushOptions) extends SimpleSendPushClient {

  var client = WebsocketPushClient(options.pushUrl)

  override def send(data: PushDataMessage): Unit = {
    if (client != null && data != null) {
      client.send(JsonUtils.stringify(data))
    }
  }

  override def close(): Unit = {
    if (client != null) {
      client.close()
    }
  }

}

object WebsocketPushClient {

  def apply(url: String): WebSocketClient = {
    val uri = URI.create(url)
    val port = if (uri.getPort == -1) {
      if ("wss" == uri.getScheme) 443 else 80
    } else {
      uri.getPort
    }
    val sslCtx: SslContext = if ("wss" == uri.getScheme) {
      SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
    } else {
      null
    }
    val client = new WebSocketClient(uri, port, sslCtx)
    client.connect()
    client
  }

  class WebSocketClient(uri: URI, port: Int, sslCtx: SslContext) extends NettyPushClient {
    val logger = Logger(getClass)

    override def connect(): Unit = {
      try {
        val handler = new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(
          uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders())
        )
        val bootstrap = new Bootstrap()
        bootstrap.group(group)
          .channel(classOf[NioSocketChannel])
          .handler(new ChannelInitializer[SocketChannel] {
            override def initChannel(ch: SocketChannel): Unit = {
              val pipeline = ch.pipeline()
              if (sslCtx != null) pipeline.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost, port))
              pipeline.addLast(new HttpClientCodec())
              pipeline.addLast(new HttpObjectAggregator(8192))
              pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE)
              pipeline.addLast(handler)
            }
          })
        bootstrap.connect(uri.getHost, port).addListener(newConnectFutureListener())
      } catch {
        case t: Throwable =>
          logger.warn("Connection error", t)
          reconnect()
      }
    }

    def send(text: String): Unit = {
      if (chn != null && chn.isActive && text != null) {
        chn.writeAndFlush(new TextWebSocketFrame(text))
      }
    }

    def send(bytes: Array[Byte]): Unit = {
      if (chn != null && chn.isActive && bytes != null) {
        chn.writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(bytes)))
      }
    }
  }

  class WebSocketClientHandler(handshaker: WebSocketClientHandshaker) extends SimpleChannelInboundHandler[Object] {
    override def channelActive(ctx: ChannelHandlerContext): Unit = {
      handshaker.handshake(ctx.channel())
    }

    override def channelRead0(ctx: ChannelHandlerContext, msg: Object): Unit = {
      if (!handshaker.isHandshakeComplete) {
        handshaker.finishHandshake(ctx.channel(), msg.asInstanceOf[FullHttpResponse])
      } else {
        msg match {
          case frame: TextWebSocketFrame =>
            val message = JsonUtils.parse(frame.text(), classOf[PushDataMessage])
            CliSystem.sendToPool(message)
          case _ => // ignored
        }
      }
    }
  }

}
