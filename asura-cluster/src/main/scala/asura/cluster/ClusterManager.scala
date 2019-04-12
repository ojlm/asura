package asura.cluster

import akka.actor.{ActorRef, ActorSystem}
import asura.cluster.actor.MemberListenerActor
import com.typesafe.config.{Config, ConfigFactory}

object ClusterManager {

  private var isIndependentSystem = false
  var enabled = false
  var system: ActorSystem = null
  var clusterManagerActor: ActorRef = null

  def init(
            config: Config = ConfigFactory.load(),
            name: String = "ClusterSystem",
            actorSystem: ActorSystem = null
          ): Unit = {
    enabled = true
    system = if (null != actorSystem) {
      actorSystem
    } else {
      isIndependentSystem = true
      ActorSystem(name, config)
    }
    clusterManagerActor = system.actorOf(MemberListenerActor.props())
  }

  def shutdown(): Unit = {
    if (null != system && !isIndependentSystem) system.terminate()
  }
}
