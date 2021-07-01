package asura.ui.cli.server

import java.nio.ByteOrder

import asura.ui.cli.push.TcpPushClient.{PushDataMessageCodec, PushDataMessageHandler}
import karate.io.netty.bootstrap.ServerBootstrap
import karate.io.netty.channel.ChannelInitializer
import karate.io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerDomainSocketChannel}
import karate.io.netty.channel.nio.NioEventLoopGroup
import karate.io.netty.channel.socket.SocketChannel
import karate.io.netty.channel.socket.nio.NioServerSocketChannel
import karate.io.netty.channel.unix.{DomainSocketAddress, UnixChannel}
import karate.io.netty.handler.codec.LengthFieldBasedFrameDecoder

object TcpServer {

  def main(args: Array[String]): Unit = {
    tcp()
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

}
