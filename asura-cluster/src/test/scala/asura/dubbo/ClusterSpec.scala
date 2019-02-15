package asura.dubbo

import akka.actor.ActorSystem
import asura.cluster.actor.ClusterManagerActor
import com.typesafe.config.ConfigFactory

object ClusterSpec {

  def main(args: Array[String]): Unit = {

    val config1 = ConfigFactory.parseString(config1Str)
    val system1 = ActorSystem("ClusterSystem", config1.resolve())
    val indigo1 = system1.actorOf(ClusterManagerActor.props(), "indigo")

    val config2 = ConfigFactory.parseString(config2Str)
    val system2 = ActorSystem("ClusterSystem", config2.resolve())
  }

  val config1Str =
    """
      |akka {
      |  actor {
      |    provider = cluster
      |  }
      |  remote {
      |    log-remote-lifecycle-events = off
      |    artery {
      |      enabled = on
      |      transport = aeron-udp
      |      canonical.hostname = "127.0.0.1"
      |      canonical.port = 2551
      |    }
      |  }
      |  cluster {
      |    seed-nodes = [
      |      "akka://ClusterSystem@127.0.0.1:2551",
      |    ]
      |    roles = [
      |      "indigo"
      |    ]
      |  }
      |}
    """.stripMargin


  val config2Str =
    """
      |akka {
      |  actor {
      |    provider = cluster
      |  }
      |  remote {
      |    log-remote-lifecycle-events = off
      |    artery {
      |      enabled = on
      |      transport = aeron-udp
      |      canonical.hostname = "127.0.0.1"
      |      canonical.port = 2552
      |    }
      |  }
      |  cluster {
      |    seed-nodes = [
      |      "akka://ClusterSystem@127.0.0.1:2551",
      |    ]
      |    roles = [
      |      "indigo"
      |    ]
      |  }
      |}
    """.stripMargin
}
