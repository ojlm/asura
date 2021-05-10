package asura.ui.cli.server

import asura.ui.cli.hub.{FrameSink, StreamFrame, StreamHub}

object ServerSpec {

  def main(args: Array[String]): Unit = {
    val s = Server(8080, ServerProxyConfig(true, 9221, 5901, true, false))
    s.start()
    logDevice("HD1910")
    println("server started.")
  }

  def logDevice(name: String): Unit = {
    StreamHub.enter(name, new FrameSink {
      override def write(frame: StreamFrame): Boolean = {
        println(s"$name, pts: ${frame.pts}, size: ${frame.size}, buf: ${frame.content().readableBytes()}")
        true
      }

      override def close(): Unit = {}
    })
  }

}
