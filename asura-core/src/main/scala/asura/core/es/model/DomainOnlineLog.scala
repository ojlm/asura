package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition}

case class DomainOnlineLog(
                            val name: String,
                            val tag: String,
                            val count: Long,
                            var coverage: Int,
                            val date: String,
                          ) {

  def generateDocId() = s"${name}_${date}${if (StringUtils.isNotEmpty(tag)) s"_${tag}" else StringUtils.EMPTY}"
}

object DomainOnlineLog extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}domain-online-log"
  override val shards: Int = 5
  override val replicas: Int = 1

  val mappings: MappingDefinition = Es6MappingDefinition(
    Seq(
      KeywordField(name = FieldKeys.FIELD_NAME),
      KeywordField(name = FieldKeys.FIELD_TAG),
      BasicField(name = FieldKeys.FIELD_COUNT, `type` = "long"),
      BasicField(name = FieldKeys.FIELD_COVERAGE, `type` = "integer"),
      KeywordField(name = FieldKeys.FIELD_DATE),
    )
  )
}
