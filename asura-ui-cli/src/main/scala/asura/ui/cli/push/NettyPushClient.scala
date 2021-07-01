package asura.ui.cli.push

import java.util.concurrent.TimeUnit

import karate.io.netty.channel.nio.NioEventLoopGroup
import karate.io.netty.channel.{Channel, ChannelFuture, ChannelFutureListener, EventLoopGroup}

trait NettyPushClient {

  val group: EventLoopGroup = new NioEventLoopGroup(1)
  var chn: Channel = null

  def newConnectFutureListener(): ChannelFutureListener = {
    new ChannelFutureListener() {
      override def operationComplete(f: ChannelFuture): Unit = {
        if (f.isSuccess) {
          chn = f.channel()
          chn.closeFuture().addListener(new ChannelFutureListener() {
            override def operationComplete(f: ChannelFuture): Unit = {
              reconnect()
            }
          })
        } else {
          reconnect()
        }
      }
    }
  }

  def connect(): Unit

  def reconnect(): Unit = {
    group.schedule(new Runnable {
      override def run(): Unit = {
        connect()
      }
    }, 10, TimeUnit.SECONDS)
  }

  def close(): Unit = {
    chn.close()
    group.shutdownGracefully()
  }

}
