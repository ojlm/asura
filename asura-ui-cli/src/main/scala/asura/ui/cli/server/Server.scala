package asura.ui.cli.server

import asura.common.util.LogUtils
import com.typesafe.scalalogging.Logger
import karate.io.netty.bootstrap.ServerBootstrap
import karate.io.netty.channel.ChannelOption
import karate.io.netty.channel.nio.NioEventLoopGroup
import karate.io.netty.channel.socket.nio.NioServerSocketChannel
import karate.io.netty.handler.ssl.util.SelfSignedCertificate
import karate.io.netty.handler.ssl.{SslContext, SslContextBuilder}

case class Server(port: Int, proxyConfig: ServerProxyConfig) {

  private val logger = Logger("Server")
  private val sslCtx: SslContext = {
    val sc = new SelfSignedCertificate()
    SslContextBuilder
      .forServer(sc.certificate(), sc.privateKey())
      .build()
  }
  private var serverThread = new Thread() {
    override def run(): Unit = {
      val bossGroup = new NioEventLoopGroup()
      val workGroup = new NioEventLoopGroup()
      try {
        val bootstrap = new ServerBootstrap()
        bootstrap.group(bossGroup, workGroup)
          .channel(classOf[NioServerSocketChannel])
          .option(ChannelOption.SO_BACKLOG, Int.box(1024))
          .option(ChannelOption.SO_REUSEADDR, Boolean.box(true))
          .childHandler(new ServerInitializer(sslCtx, proxyConfig))
        logger.info(s"server listen on $port")
        bootstrap.bind(port).sync().channel().closeFuture().sync()
      } catch {
        case _: InterruptedException =>
          logger.info("server is stopped.")
        case t: Throwable =>
          logger.warn(LogUtils.stackTraceToString(t))
          sys.exit(0)
      } finally {
        bossGroup.shutdownGracefully()
        workGroup.shutdownGracefully()
      }
    }
  }

  def start(): Unit = {
    if (serverThread != null) {
      serverThread.start()
    }
  }

  def stop(): Unit = {
    if (serverThread != null) {
      serverThread.interrupt()
      serverThread = null
    }
  }

}
