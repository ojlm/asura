package asura.common

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.scalalogging.Logger
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AkkaTestKitBaseSpec() extends TestKit(ActorSystem("TestActorSystem"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  val logger = Logger(this.getClass)
  implicit val dispatcher = system.dispatcher

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
