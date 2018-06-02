package asura.core.actor

import asura.common.AkkaTestKitBaseSpec
import asura.core.CoreConfig
import asura.core.actor.messages.SenderMessage
import asura.core.es.EsClientConfig
import asura.core.job.JobTestConfig
import asura.core.job.actor.JobCiActor

import scala.concurrent.duration._

class JobCiActorSpec extends AkkaTestKitBaseSpec with EsClientConfig with JobTestConfig {

  CoreConfig.system = system
  CoreConfig.dispatcher = system.dispatcher

  "test ci actor" in {
    val ciActor = system.actorOf(JobCiActor.props("default_indigo_ci"))
    ciActor ! SenderMessage(self)
    receiveWhile(10 seconds) {
      case event: String =>
        println(event)
    }
  }
}
