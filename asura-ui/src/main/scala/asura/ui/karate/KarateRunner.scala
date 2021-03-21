package asura.ui.karate

import com.intuit.karate.core._
import com.intuit.karate.resource.MemoryResource
import com.intuit.karate.{Actions, FileUtils, ScenarioActions}

object KarateRunner {

  def runFeature(text: String): FeatureResult = {
    val resource = new MemoryResource(FileUtils.WORKING_DIR, text)
    val feature = Feature.read(resource)
    val featureRuntime = FeatureRuntime.of(feature)
    featureRuntime.run()
    featureRuntime.result
  }

  def buildScenarioEngine(): ScenarioEngine = {
    ScenarioEngine.forTempUse()
  }

  def buildScenarioAction(engine: ScenarioEngine = ScenarioEngine.forTempUse()): Actions = {
    new ScenarioActions(engine)
  }

  def executeStep(step: String)(implicit actions: Actions): KarateResult = {
    StepRuntimeEx.execute(step, actions)
  }

  def executeSteps(steps: Seq[String])(implicit actions: Actions): Seq[KarateResult] = {
    steps.map(s => executeStep(s))
  }

  def parseFeatureSummary(text: String): KarateFeatureSummary = {
    val resource = new MemoryResource(FileUtils.WORKING_DIR, text)
    val feature = Feature.read(resource)
    KarateFeatureSummary(
      name = feature.getName,
      description = feature.getDescription,
      lineCount = if (feature.getLine != null) feature.getLine else 0,
      sectionCount = if (feature.getSections != null) feature.getSections.size() else 0,
    )
  }
}
