package asura.ui.cli.push

import java.net.{InetSocketAddress, SocketAddress, URI}
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util

import asura.common.util.JsonUtils
import asura.ui.cli.CliSystem
import com.typesafe.scalalogging.Logger
import karate.io.netty.bootstrap.Bootstrap
import karate.io.netty.buffer.{ByteBuf, ByteBufUtil}
import karate.io.netty.channel._
import karate.io.netty.channel.epoll.{EpollDomainSocketChannel, EpollEventLoopGroup}
import karate.io.netty.channel.nio.NioEventLoopGroup
import karate.io.netty.channel.socket.SocketChannel
import karate.io.netty.channel.socket.nio.NioSocketChannel
import karate.io.netty.channel.unix.{DomainSocketAddress, UnixChannel}
import karate.io.netty.handler.codec.{ByteToMessageDecoder, LengthFieldBasedFrameDecoder, MessageToMessageEncoder}

case class TcpPushClient(options: PushOptions) extends SimpleSendPushClient {

  var client = TcpPushClient(options.pushUrl)

  override def send(data: PushDataMessage): Unit = {
    client.send(data)
  }

  override def close(): Unit = {
    if (client != null) {
      client.close()
    }
  }

}

object TcpPushClient {

  def apply(url: String): TcpClient = {
    val client = new TcpClient(URI.create(url))
    client.connect()
    client
  }

  class TcpClient(uri: URI) extends NettyPushClient {

    val logger = Logger(getClass)

    override val group = if (uri.getScheme == "unix") new EpollEventLoopGroup(1) else new NioEventLoopGroup(1)

    def addHandler(ch: Channel): Unit = {
      val pipeline = ch.pipeline()
      pipeline.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE, 0, 4, 0, 4, true))
      pipeline.addLast(new PushDataMessageCodec())
      pipeline.addLast(new PushDataMessageHandler())
    }

    def connect(): Unit = {
      try {
        val bootstrap = new Bootstrap()
        bootstrap.group(group)
        var address: SocketAddress = null
        uri.getScheme match {
          case "tcp" =>
            address = InetSocketAddress.createUnresolved(uri.getHost, uri.getPort)
            bootstrap.channel(classOf[NioSocketChannel])
            bootstrap.handler(new ChannelInitializer[SocketChannel] {
              override def initChannel(ch: SocketChannel): Unit = {
                addHandler(ch)
              }
            })
          case "unix" =>
            address = new DomainSocketAddress(uri.getPath)
            bootstrap.channel(classOf[EpollDomainSocketChannel])
            bootstrap.handler(new ChannelInitializer[UnixChannel] {
              override def initChannel(ch: UnixChannel): Unit = {
                addHandler(ch)
              }
            })
        }
        bootstrap.connect(address).addListener(newConnectFutureListener())
      } catch {
        case t: Throwable =>
          logger.warn("Connection error", t)
          reconnect()
      }
    }

    def send(data: PushDataMessage): Unit = {
      if (chn != null && chn.isActive && data != null) {
        chn.writeAndFlush(data)
      }
    }

  }

  class PushDataMessageCodec() extends CombinedChannelDuplexHandler[PushDataMessageDecoder, PushDataMessageEncoder] {
    init(new PushDataMessageDecoder(), new PushDataMessageEncoder())
  }

  class PushDataMessageDecoder extends ByteToMessageDecoder {
    override def decode(ctx: ChannelHandlerContext, buf: ByteBuf, list: util.List[AnyRef]): Unit = {
      val message = JsonUtils.parse(new String(ByteBufUtil.getBytes(buf), StandardCharsets.UTF_8), classOf[PushDataMessage])
      buf.skipBytes(buf.readableBytes())
      list.add(message)
    }
  }

  class PushDataMessageEncoder extends MessageToMessageEncoder[PushDataMessage] {
    override def encode(ctx: ChannelHandlerContext, msg: PushDataMessage, list: util.List[AnyRef]): Unit = {
      val bytes = JsonUtils.stringify(msg).getBytes(StandardCharsets.UTF_8)
      val buf = ctx.channel().alloc().heapBuffer(bytes.length + 4)
      buf.writeInt(bytes.length)
      buf.writeBytes(bytes)
      list.add(buf)
    }
  }

  class PushDataMessageHandler() extends SimpleChannelInboundHandler[PushDataMessage] {
    override def channelRead0(ctx: ChannelHandlerContext, msg: PushDataMessage): Unit = {
      CliSystem.sendToPool(msg)
    }
  }

}
