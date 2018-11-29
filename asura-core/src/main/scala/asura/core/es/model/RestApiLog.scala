package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{KeywordField, MappingDefinition}

case class RestApiLog(
                       val domain: String,
                       val method: String,
                       val urlPath: String,
                       val count: Int,
                     )

object RestApiLog extends IndexSetting {

  val INDEX_DATE_TIME_PATTERN = "yyyy.MM.dd"
  val Index: String = s"${EsConfig.IndexPrefix}rest-api-log"
  override val shards: Int = 5
  override val replicas: Int = 0

  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_DOMAIN),
      KeywordField(name = FieldKeys.FIELD_METHOD),
      KeywordField(name = FieldKeys.FIELD_URL_PATH),
      KeywordField(name = FieldKeys.FIELD_COUNT),
    )
  )
}
