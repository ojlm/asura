package asura.dubbo.telnet

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Framing, Tcp}
import akka.util.ByteString
import com.typesafe.scalalogging.Logger

object TelnetEchoApp {

  val logger = Logger("TelnetEchoApp")
  implicit val system = ActorSystem("telnet")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val echo = Flow[ByteString]
    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = false))
    .map(_.utf8String)
    .map(txt => {
      logger.info(s"got(${txt.length}):${txt}")
      txt + "\n"
    })
    .map(ByteString(_))

  def main(args: Array[String]): Unit = {
    val connections = Tcp().bind("127.0.0.1", 8888)
    connections runForeach { connection =>
      logger.info(s"New connection from: ${connection.remoteAddress}")
      connection.handleWith(echo)
    }
  }
}
