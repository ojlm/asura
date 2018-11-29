package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{KeywordField, MappingDefinition}

case class DomainOnlineLog(
                            val name: String,
                            val count: Int,
                            val date: String,
                          )

object DomainOnlineLog extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}domain-online-log"
  override val shards: Int = 5
  override val replicas: Int = 0

  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_NAME),
      KeywordField(name = FieldKeys.FIELD_COUNT),
      KeywordField(name = FieldKeys.FIELD_DATE),
    )
  )
}
