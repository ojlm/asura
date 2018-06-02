package asura.common

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AkkaTestKitBaseSpec() extends TestKit(ActorSystem("TestActorSystem"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val dispatcher = system.dispatcher

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
