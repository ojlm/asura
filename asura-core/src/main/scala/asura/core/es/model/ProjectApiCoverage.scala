package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition}

case class ProjectApiCoverage(
                               val group: String,
                               val project: String,
                               val domain: String,
                               val tag: String,
                               val date: String,
                               val coverage: Int,
                             ) {

  def generateDocId() = s"${group}_${project}_${domain}_${date}${if (StringUtils.isNotEmpty(tag)) s"_${tag}" else StringUtils.EMPTY}"
}

object ProjectApiCoverage extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}project-api-coverage"
  override val shards: Int = 5
  override val replicas: Int = 1

  val mappings: MappingDefinition = Es6MappingDefinition(
    Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_DOMAIN),
      KeywordField(name = FieldKeys.FIELD_TAG),
      KeywordField(name = FieldKeys.FIELD_DATE),
      BasicField(name = FieldKeys.FIELD_COVERAGE, `type` = "integer"),
    )
  )
}
