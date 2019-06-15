package asura.pea.gatling

import asura.pea.actor.GatlingRunnerActor
import asura.pea.actor.GatlingRunnerActor.StartMessage
import asura.pea.{IDEPathHelper, PeaAppSpec}

object RunnerSpec extends PeaAppSpec {

  def main(args: Array[String]): Unit = {
    val message = StartMessage(
      IDEPathHelper.binariesFolder.toAbsolutePath.toString,
      IDEPathHelper.resultsFolder.toAbsolutePath.toString,
      "simulations.BasicSimulation"
    )
    val code = GatlingRunnerActor.start(message)
    logger.info(s"Exit: ${code}")
  }
}
