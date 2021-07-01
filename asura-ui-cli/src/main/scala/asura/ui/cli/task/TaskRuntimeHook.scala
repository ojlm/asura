package asura.ui.cli.task

import java.util.concurrent.atomic.AtomicBoolean

import com.intuit.karate.RuntimeHook
import com.intuit.karate.core.{ScenarioRuntime, Step}

class TaskRuntimeHook(stop: AtomicBoolean = new AtomicBoolean(false)) extends RuntimeHook {

  def stop(): Unit = {
    stop.set(true)
  }

  override def beforeStep(step: Step, sr: ScenarioRuntime): Boolean = {
    !stop.get()
  }

}
