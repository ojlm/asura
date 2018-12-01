package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition}

case class RestApiOnlineLog(
                             val domain: String,
                             val method: String,
                             val urlPath: String,
                             val count: Int,
                           )

object RestApiOnlineLog extends IndexSetting {

  val INDEX_DATE_TIME_PATTERN = "yyyy.MM.dd"
  val Index: String = s"${EsConfig.IndexPrefix}rest-api-online-log"
  override val shards: Int = 5
  override val replicas: Int = 0

  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_DOMAIN),
      KeywordField(name = FieldKeys.FIELD_METHOD),
      KeywordField(name = FieldKeys.FIELD_URL_PATH),
      BasicField(name = FieldKeys.FIELD_COUNT, `type` = "integer"),
    )
  )
}
