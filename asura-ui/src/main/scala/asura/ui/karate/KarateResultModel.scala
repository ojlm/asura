package asura.ui.karate

import com.intuit.karate.FileUtils

case class KarateResultModel(
                              version: String = FileUtils.KARATE_VERSION,
                              threads: Int,
                              featuresPassed: Int,
                              featuresFailed: Int,
                              featuresSkipped: Int,
                              scenariosPassed: Int,
                              scenariosFailed: Int,
                              elapsedTime: Long,
                              totalTime: Long,
                              efficiency: Double,
                              resultDate: Long,
                              featureSummary: java.util.List[FeatureResultModel],
                            )
