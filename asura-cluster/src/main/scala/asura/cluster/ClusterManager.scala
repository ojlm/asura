package asura.cluster

import akka.actor.{ActorRef, ActorSystem}
import asura.cluster.actor.ClusterManagerActor
import com.typesafe.config.Config

object ClusterManager {

  var enabled = false
  var system: ActorSystem = null
  var clusterManagerActor: ActorRef = null

  def init(config: Config): Unit = {
    enabled = true
    system = ActorSystem("ClusterSystem", config)
    clusterManagerActor = system.actorOf(ClusterManagerActor.props())
  }

  def shutdown(): Unit = {
    if (null != system) system.terminate()
  }
}
