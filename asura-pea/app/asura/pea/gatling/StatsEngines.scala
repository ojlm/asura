package asura.pea.gatling

import akka.actor.ActorSystem
import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.writer.RunMessage
import io.gatling.core.stats.{DataWritersStatsEngine, StatsEngine}

object StatsEngines {

  def newStatsEngine(
                      simulationParams: SimulationParams,
                      runMessage: RunMessage,
                      system: ActorSystem,
                      clock: Clock,
                      configuration: GatlingConfiguration,
                      engineType: String = TYPE_DATA_WRITERS,
                    ): StatsEngine = {
    engineType match {
      case TYPE_NOOP => NoopStatsEngine(simulationParams, runMessage, system, clock, configuration)
      case _ => DataWritersStatsEngine(simulationParams, runMessage, system, clock, configuration)
    }
  }

  val TYPE_DATA_WRITERS = "data-writers"
  val TYPE_NOOP = "noop"
}
