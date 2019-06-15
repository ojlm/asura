package asura.pea.gatling

import akka.actor.ActorSystem
import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.writer.RunMessage
import io.gatling.core.stats.{DataWritersStatsEngine, StatsEngine}

object StatsEngine {

  def newStatsEngine(
                      simulationParams: SimulationParams,
                      runMessage: RunMessage,
                      system: ActorSystem,
                      clock: Clock,
                      configuration: GatlingConfiguration,
                    ): StatsEngine = {
    DataWritersStatsEngine(simulationParams, runMessage, system, clock, configuration)
  }

}
