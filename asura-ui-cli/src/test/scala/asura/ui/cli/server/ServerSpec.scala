package asura.ui.cli.server

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.codec.VideoStream
import asura.ui.cli.hub.{FrameSink, StreamFrame, StreamHub}

object ServerSpec {

  def main(args: Array[String]): Unit = {
    val s = Server(8080, ServerProxyConfig(true, 9221, 5901, true, false))
    s.start()
    logDevice("HD1910")
    println("server started.")
  }

  def logDevice(name: String): Unit = {
    val stream = VideoStream.init(null)
    StreamHub.enter(name, new FrameSink {
      override def write(frame: StreamFrame): Boolean = {
        stream.put(frame)
        true
      }

      override def close(): Unit = {}
    })
    Future {
      stream.run()
    }(ExecutionContext.global)
  }

}
