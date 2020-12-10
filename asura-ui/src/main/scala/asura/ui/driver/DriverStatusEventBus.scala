package asura.ui.driver

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import asura.ui.driver.DriverStatusEventBus.PublishDriverStatusMessage

class DriverStatusEventBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

  override protected def classify(event: PublishDriverStatusMessage): Classifier = event.ref

  override protected def mapSize: Int = 1

  override type Event = PublishDriverStatusMessage

}

object DriverStatusEventBus {

  case class PublishDriverStatusMessage(ref: ActorRef, status: DriverStatus)

}
