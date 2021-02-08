package asura.ui.driver

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import asura.ui.actor.ChromeDriverHolderActor.DriverDevToolsMessage
import asura.ui.driver.DriverDevToolsEventBus.PublishDriverDevToolsMessage

class DriverDevToolsEventBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

  override protected def classify(event: PublishDriverDevToolsMessage): Classifier = event.ref

  override protected def mapSize: Int = 1

  override type Event = PublishDriverDevToolsMessage

}

object DriverDevToolsEventBus {

  case class PublishDriverDevToolsMessage(ref: ActorRef, data: DriverDevToolsMessage)

}
