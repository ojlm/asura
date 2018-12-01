package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition}

case class DomainOnlineLog(
                            val name: String,
                            val count: Int,
                            val date: String,
                          ) {

  def generateDocId() = s"${name}_${date}"
}

object DomainOnlineLog extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}domain-online-log"
  override val shards: Int = 5
  override val replicas: Int = 1

  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_NAME),
      BasicField(name = FieldKeys.FIELD_COUNT, `type` = "integer"),
      KeywordField(name = FieldKeys.FIELD_DATE),
    )
  )
}
