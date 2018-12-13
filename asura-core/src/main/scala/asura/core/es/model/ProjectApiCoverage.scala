package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition}

class ProjectApiCoverage(
                          val group: String,
                          val project: String,
                          val domain: String,
                          val date: String,
                          val coverage: Long,
                        ) {

  def generateDocId() = s"${group}_${project}_${domain}_${date}"
}

object ProjectApiCoverage extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}project-api-coverage"
  override val shards: Int = 5
  override val replicas: Int = 1

  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_DOMAIN),
      KeywordField(name = FieldKeys.FIELD_DATE),
      BasicField(name = FieldKeys.FIELD_COVERAGE, `type` = "long"),
    )
  )
}
