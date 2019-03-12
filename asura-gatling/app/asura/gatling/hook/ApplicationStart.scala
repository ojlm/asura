package asura.gatling.hook

import akka.actor.ActorSystem
import asura.cluster.ClusterManager
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class ApplicationStart @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  system: ActorSystem,
                                  configuration: Configuration,
                                ) {

  ClusterManager.init(config = configuration.underlying, actorSystem = system)

  // add stop hook
  lifecycle.addStopHook { () =>
    Future {
      ClusterManager.shutdown()
    }(system.dispatcher)
  }
}
