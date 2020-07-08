package asura.common

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.scalalogging.Logger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class AkkaTestKitBaseSpec() extends TestKit(ActorSystem("TestActorSystem"))
  with ImplicitSender with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  val logger = Logger(this.getClass)
  implicit val dispatcher = system.dispatcher

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
