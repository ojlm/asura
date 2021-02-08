//package asura.ui.command
//
//import java.util.Collections
//import java.util.concurrent.atomic.AtomicBoolean
//
//import akka.actor.ActorRef
//import asura.common.util.{JsonUtils, LogUtils, StringUtils}
//import asura.ui.command.KarateCommandRunner.KarateCommandParams
//import asura.ui.driver.{DriverCommand, DriverCommandEnd, DriverCommandLog}
//import asura.ui.karate.KarateRunner
//import com.intuit.karate.core._
//import com.intuit.karate.http.HttpRequestBuilder
//import com.intuit.karate.{FileUtils, Results}
//
//case class KarateCommandRunner(
//                                command: DriverCommand,
//                                stopNow: AtomicBoolean,
//                                logActor: ActorRef,
//                              ) extends CommandRunner with ExecutionHook {
//
//  val meta = command.meta
//
//  val params = JsonUtils.mapper.convertValue(command.params, classOf[KarateCommandParams])
//
//  override def run(): DriverCommandEnd = {
//    if (StringUtils.isEmpty(params.text)) {
//      DriverCommandEnd(Commands.KARATE, false, "Empty content")
//    } else {
//      val result = KarateRunner.runFeature(params.text, Collections.emptyMap(), this)
//      DriverCommandEnd(Commands.KARATE, true, result = result)
//    }
//  }
//
//  def continue(): Boolean = {
//    !stopNow.get()
//  }
//
//  override def beforeScenario(scenario: Scenario, context: ScenarioContext): Boolean = {
//    if (logActor != null) logActor ! DriverCommandLog(Commands.KARATE, "scenario", s"${scenario.getName} start", meta)
//    continue()
//  }
//
//  override def afterScenario(result: ScenarioResult, context: ScenarioContext): Unit = {
//    if (logActor != null) logActor ! DriverCommandLog(Commands.KARATE, "scenario", s"${result.getScenario.getName} end", meta)
//  }
//
//  override def beforeFeature(feature: Feature, context: ExecutionContext): Boolean = {
//    continue()
//  }
//
//  override def afterFeature(result: FeatureResult, context: ExecutionContext): Unit = {
//  }
//
//  override def beforeAll(results: Results): Unit = {
//  }
//
//  override def afterAll(results: Results): Unit = {
//    if (logActor != null && results != null) {
//      logActor ! DriverCommandLog(Commands.KARATE, "result", KarateCommandRunner.resultsToString(results), meta)
//    }
//  }
//
//  override def beforeStep(step: Step, context: ScenarioContext): Boolean = {
//    continue()
//  }
//
//  override def afterStep(result: StepResult, context: ScenarioContext): Unit = {}
//
//  override def getPerfEventName(req: HttpRequestBuilder, context: ScenarioContext): String = {
//    null
//  }
//
//  override def reportPerfEvent(event: PerfEvent): Unit = {}
//}
//
//object KarateCommandRunner {
//
//  case class KarateCommandParams(text: String)
//
//  def resultsToString(results: Results): String = {
//    val map = results.toMap()
//    val sb = new StringBuilder()
//    sb.append("Karate version: ").append(FileUtils.getKarateVersion).append("\n")
//    sb.append("======================================================").append("\n")
//    sb.append(String.format(
//      "elapsed: %6.2f | threads: %4d | thread time: %.2f\n",
//      results.getElapsedTime() / 1000,
//      1, results.getTimeTakenMillis() / 1000
//    ))
//    sb.append(String.format(
//      "features: %5d | ignored: %4d | efficiency: %.2f\n",
//      map.get("features"), map.get("ignored"), map.get("efficiency")))
//    sb.append(String.format(
//      "scenarios: %4d | passed: %5d | failed: %d\n",
//      map.get("scenarios"), map.get("passed"), map.get("failed")
//    ))
//    sb.append("======================================================\n")
//    sb.append(results.getErrorMessages()).append("\n")
//    if (results.getFailureReason() != null) {
//      sb.append("*** runner exception stack trace ***\n")
//      sb.append(LogUtils.stackTraceToString(results.getFailureReason())).append("\n")
//    }
//    sb.toString()
//  }
//
//}
