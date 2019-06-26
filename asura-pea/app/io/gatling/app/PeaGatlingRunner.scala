/**
  * custom pea-app code from gatling.
  */
package io.gatling.app

import akka.actor.ActorSystem
import akka.pattern.ask
import asura.pea.actor.GatlingRunnerActor.PeaGatlingRunResult
import asura.pea.gatling.StatsEngines
import com.typesafe.scalalogging.StrictLogging
import io.gatling.commons.util.DefaultClock
import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.inject.Injector
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.controller.{Controller, ControllerCommand}
import io.gatling.core.scenario.{Scenario, SimulationParams}
import io.gatling.core.stats.writer.RunMessage

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

class PeaGatlingRunner(config: mutable.Map[String, _]) extends StrictLogging {

  val clock = new DefaultClock
  val configuration = GatlingConfiguration.load(config)
  val system = ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())

  io.gatling.core.Predef.clock = clock
  io.gatling.core.Predef._configuration = configuration

  def run()(implicit ec: ExecutionContext): PeaGatlingRunResult = {
    val selection = Selection(None, configuration)
    val simulation = selection.simulationClass.getDeclaredConstructor().newInstance()
    logger.info("Simulation instantiated")
    val simulationParams = simulation.params(configuration)
    logger.info("Simulation params built")

    simulation.executeBefore()
    logger.info("Before hooks executed")

    val runMessage = RunMessage(simulationParams.name, selection.simulationId, clock.nowMillis, selection.description, configuration.core.version)

    val code = Future {
      val runResult = try {
        val statsEngine = StatsEngines.newStatsEngine(simulationParams, runMessage, system, clock, configuration)
        val throttler = Throttler(system, simulationParams)
        val injector = Injector(system, statsEngine, clock)
        val controller = system.actorOf(Controller.props(statsEngine, injector, throttler, simulationParams, configuration), Controller.ControllerActorName)
        val exit = new Exit(injector, clock)
        val coreComponents = CoreComponents(system, controller, throttler, statsEngine, clock, exit, configuration)
        logger.info("CoreComponents instantiated")
        val scenarios = simulationParams.scenarios(coreComponents)
        start(simulationParams, scenarios, coreComponents) match {
          case Failure(t) => throw t
          case _ =>
            simulation.executeAfter()
            logger.info("After hooks executed")
            RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
        }

      } catch {
        case t: Throwable =>
          logger.error("Run crashed", t)
          throw t
      } finally {
        terminateActorSystem()
      }
      new RunResultProcessor(configuration).processRunResult(runResult).code
    }
    PeaGatlingRunResult(runMessage.runId, code)
  }

  private def start(simulationParams: SimulationParams, scenarios: List[Scenario], coreComponents: CoreComponents): Try[_] = {
    val timeout = Int.MaxValue.milliseconds - 10.seconds
    val start = coreComponents.clock.nowMillis
    logger.info(s"Simulation ${simulationParams.name} started...")
    logger.info("Asking Controller to start")
    val whenRunDone: Future[Try[String]] = coreComponents.controller.ask(ControllerCommand.Start(scenarios))(timeout).mapTo[Try[String]]
    val runDone = Await.result(whenRunDone, timeout)
    logger.info(s"Simulation ${simulationParams.name} completed in ${(coreComponents.clock.nowMillis - start) / 1000} seconds")
    runDone
  }

  private def terminateActorSystem(): Unit = {
    try {
      val whenTerminated = system.terminate()
      Await.result(whenTerminated, configuration.core.shutdownTimeout milliseconds)
    } catch {
      case NonFatal(e) =>
        logger.debug("Could not terminate ActorSystem", e)
    }
  }

}

object PeaGatlingRunner extends StrictLogging {

  def apply(config: mutable.Map[String, _]): PeaGatlingRunner = new PeaGatlingRunner(config)

  def run(config: mutable.Map[String, _])(implicit ec: ExecutionContext): PeaGatlingRunResult = {
    PeaGatlingRunner(config).run()
  }
}
