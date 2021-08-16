package asura.ui.karate

import asura.ui.karate.model.Features

object FeatureParser {

  def main(args: Array[String]): Unit = {
    val sb = new StringBuilder()
    val features = Features(args: _*)
    sb.append("feature:name,feature:scenarios\n")
    features.features.foreach(feature => {
      sb.append(s"${feature.name},${feature.scenarios.size}\n")
    })
    sb.append(s"\n\nfeature:name,scenario:name,scenario:steps\n")
    features.features.foreach(feature => {
      feature.scenarios.foreach(scenario => {
        sb.append(s"${feature.name},${scenario.name},${scenario.steps}\n")
      })
    })
    println(sb.toString())
  }

}
