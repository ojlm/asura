package asura.ui.cli.task

import java.util.concurrent.atomic.AtomicBoolean

import asura.ui.cli.push.PushEventListener
import asura.ui.cli.push.PushEventListener.DriverCommandLogEvent
import asura.ui.cli.task.TaskLog.LogType
import com.intuit.karate.core.{FeatureRuntime, ScenarioRuntime, Step, StepResult}
import com.intuit.karate.http.{HttpRequest, Response}
import com.intuit.karate.{RuntimeHook, Suite}

class TaskRuntimeHook(
                       listener: PushEventListener,
                       task: TaskMeta,
                       stop: AtomicBoolean = new AtomicBoolean(false),
                     ) extends RuntimeHook {

  def stop(): Unit = {
    stop.set(true)
  }

  def sendLog(log: TaskLog): Unit = {
    if (listener != null && log != null) {
      listener.driverCommandLogEvent(DriverCommandLogEvent(task, log))
    }
  }

  override def beforeScenario(sr: ScenarioRuntime): Boolean = {
    sendLog(TaskLog.karate(LogType.SCENARIO, s"scenario[${sr.scenario.getName}]: start running"))
    super.beforeScenario(sr)
  }

  override def afterScenario(sr: ScenarioRuntime): Unit = {
    sendLog(TaskLog.karate(LogType.SCENARIO, s"scenario[${sr.scenario.getName}]: has finished running"))
    super.afterScenario(sr)
  }

  override def beforeFeature(fr: FeatureRuntime): Boolean = {
    sendLog(TaskLog.karate(LogType.FEATURE, s"feature[${fr.feature.getName}]: start running"))
    super.beforeFeature(fr)
  }

  override def afterFeature(fr: FeatureRuntime): Unit = {
    sendLog(TaskLog.karate(LogType.FEATURE, s"feature[${fr.feature.getName}]: has finished running"))
    super.afterFeature(fr)
  }

  override def beforeSuite(suite: Suite): Unit = {
    sendLog(TaskLog.karate(LogType.SUITE, "suit start running"))
    super.beforeSuite(suite)
  }

  override def afterSuite(suite: Suite): Unit = {
    sendLog(TaskLog.karate(LogType.SUITE, "suit has finished running"))
    super.afterSuite(suite)
  }

  override def beforeStep(step: Step, sr: ScenarioRuntime): Boolean = {
    sendLog(TaskLog.karate(LogType.STEP, s"step[${step.getLine}] start running: ${step.toString}"))
    !stop.get()
  }

  override def afterStep(result: StepResult, sr: ScenarioRuntime): Unit = {
    sendLog(TaskLog.karate(LogType.STEP, s"step[${result.getStep.getLine}] has finished running: ${result.getResult.getStatus}"))
    super.afterStep(result, sr)
  }

  override def beforeBackground(sr: ScenarioRuntime): Unit = {
    super.beforeBackground(sr)
  }

  override def afterBackground(sr: ScenarioRuntime): Unit = {
    super.afterBackground(sr)
  }

  override def beforeHttpCall(request: HttpRequest, sr: ScenarioRuntime): Unit = {
    super.beforeHttpCall(request, sr)
  }

  override def afterHttpCall(request: HttpRequest, response: Response, sr: ScenarioRuntime): Unit = {
    super.afterHttpCall(request, response, sr)
  }

}
