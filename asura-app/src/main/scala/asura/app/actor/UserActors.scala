package asura.app.actor

import asura.AppConfig
import asura.core.job.actor.SchedulerActor
import com.typesafe.scalalogging.Logger

object UserActors {
  val logger = Logger("UserActors")

  import asura.GlobalImplicits._

  val scheduler = system.actorOf(SchedulerActor.props(AppConfig.quartzParallelConfig), "JobScheduler")

  def init(): Unit = {
    logger.info("init user actors")
  }
}
