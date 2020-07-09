package asura.core.model

case class RecommendProject(
                             group: String,
                             project: String,
                             count: Long,
                             var summary: String = null,
                             var description: String = null,
                           )
