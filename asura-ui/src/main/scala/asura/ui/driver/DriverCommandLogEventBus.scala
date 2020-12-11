package asura.ui.driver

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import asura.ui.driver.DriverCommandLogEventBus.PublishCommandLogMessage

class DriverCommandLogEventBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

  override protected def classify(event: PublishCommandLogMessage): Classifier = event.ref

  override protected def mapSize: Int = 1

  override type Event = PublishCommandLogMessage

}

object DriverCommandLogEventBus {

  case class PublishCommandLogMessage(ref: ActorRef, msg: DriverCommandLog)

}
