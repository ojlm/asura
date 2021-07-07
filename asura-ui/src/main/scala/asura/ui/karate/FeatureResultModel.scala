package asura.ui.karate

case class FeatureResultModel(
                               failed: Boolean,
                               name: String,
                               description: String,
                               durationMillis: Long,
                               passedCount: Int,
                               failedCount: Int,
                               scenarioCount: Int,
                               packageQualifiedName: String,
                               relativePath: String,
                             )
