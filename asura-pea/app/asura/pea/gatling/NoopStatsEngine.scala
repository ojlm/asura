package asura.pea.gatling

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.stats.Status
import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.session.{GroupBlock, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer.{RunMessage, UserMessage}

class NoopStatsEngine(
                       simulationParams: SimulationParams,
                       runMessage: RunMessage,
                       system: ActorSystem,
                       clock: Clock,
                       configuration: GatlingConfiguration,
                     ) extends StatsEngine with StrictLogging {

  override def start(): Unit = {
    logger.info(s"NoopStatsEngine.start()")
  }

  override def stop(replyTo: ActorRef, exception: Option[Exception]): Unit = {
    logger.info(s"NoopStatsEngine.stop() ${replyTo.path} ${exception}")
  }

  override def logUser(userMessage: UserMessage): Unit = {
    logger.info(s"NoopStatsEngine.logUser() ${userMessage}")
  }

  override def logResponse(
                            session: Session,
                            requestName: String,
                            startTimestamp: Long,
                            endTimestamp: Long,
                            status: Status,
                            responseCode: Option[String],
                            message: Option[String]
                          ): Unit = {
    logger.info(
      s"NoopStatsEngine.logResponse() ${session} ${requestName} ${startTimestamp} ${endTimestamp} " +
        s"${status} ${responseCode} ${message}"
    )
  }

  override def logGroupEnd(session: Session, group: GroupBlock, exitTimestamp: Long): Unit = {
    logger.info(s"NoopStatsEngine.logGroupEnd() ${session} ${group} ${exitTimestamp}")
  }

  override def logCrash(session: Session, requestName: String, error: String): Unit = {
    logger.info(s"NoopStatsEngine.logCrash() ${session} ${requestName} ${error}")
  }
}

object NoopStatsEngine {

  def apply(
             simulationParams: SimulationParams,
             runMessage: RunMessage,
             system: ActorSystem,
             clock: Clock,
             configuration: GatlingConfiguration,
           ): NoopStatsEngine = {
    new NoopStatsEngine(simulationParams, runMessage, system, clock, configuration)
  }
}
