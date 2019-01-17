package asura.dubbo.telnet

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import asura.common.actor.BaseActor
import asura.dubbo.actor.TelnetClientActor
import com.typesafe.scalalogging.Logger

object TelnetClientApp {

  val logger = Logger("TelnetClientApp")
  implicit val system = ActorSystem("telnet")
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  var clientActor: ActorRef = null

  def main(args: Array[String]): Unit = {
    val echoActor = system.actorOf(Props(new Echo()))
    clientActor = system.actorOf(TelnetClientActor.props("127.0.0.1", 20880, echoActor))
  }

  class Echo() extends BaseActor {
    var isLs = false

    override def receive: Receive = {
      case msg: ByteString =>
        log.info(s"from server:${msg.utf8String}")
        if (!isLs) {
          clientActor ! ByteString("ls\r\n")
          isLs = true
        }
    }
  }

}
