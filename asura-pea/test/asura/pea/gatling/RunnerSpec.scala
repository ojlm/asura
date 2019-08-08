package asura.pea.gatling

import asura.common.util.FutureUtils.RichFuture
import asura.pea.actor.GatlingRunnerActor
import asura.pea.actor.GatlingRunnerActor.StartMessage
import asura.pea.{IDEPathHelper, PeaAppSpec}

import scala.concurrent.ExecutionContext.global

object RunnerSpec extends PeaAppSpec {

  def main(args: Array[String]): Unit = {
    report()
  }

  def run(): Unit = {
    val message = StartMessage(
      IDEPathHelper.binariesFolder.toAbsolutePath.toString,
      "simulations.BasicSimulation",
      true,
      IDEPathHelper.resultsFolder.toAbsolutePath.toString,
    )
    val code = GatlingRunnerActor.start(message)(global)
    logger.info(s"Exit: ${code}")
  }

  def report(): Unit = {
    val code = GatlingRunnerActor.generateReport(
      "runId",
      IDEPathHelper.resultsFolder.toAbsolutePath.toString,
    ).await
    logger.info(s"Exit: ${code}")
  }
}
