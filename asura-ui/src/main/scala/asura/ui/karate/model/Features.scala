package asura.ui.karate.model

import scala.collection.mutable.ArrayBuffer

import com.intuit.karate.BuilderEx

class Features(val features: Seq[Feature])

object Features {

  def apply(path: String*): Features = {
    val features = ArrayBuffer[Feature]()
    BuilderEx.paths(path: _*).resolveAll().forEach(feature => {
      val background = feature.getBackground
      val sections = feature.getSections
      val scenarios = ArrayBuffer[Scenario]()
      if (sections != null) {
        sections.forEach(section => {
          val scenario = section.getScenario
          if (scenario != null) {
            val steps = scenario.getSteps
            scenarios += Scenario(
              scenario.getName,
              scenario.getDescription,
              if (steps != null) steps.size() else 0
            )
          }
        })
      }
      features += Feature(
        feature.getName,
        feature.getDescription,
        if (background != null && background.getSteps != null) background.getSteps.size() else 0,
        scenarios.toSeq,
      )
    })
    new Features(features.toSeq)
  }

}
