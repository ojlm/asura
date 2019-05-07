package asura.core.es.model

import asura.core.model.AggsItem.Metrics
import asura.core.es.EsConfig
import asura.core.es.model.RestApiOnlineLog.GroupProject
import com.sksamuel.elastic4s.mappings._

case class RestApiOnlineLog(
                             val domain: String,
                             val tag: String,
                             val method: String,
                             val urlPath: String,
                             val count: Long,
                             val percentage: Int, // like 44.22 but saved 4422
                             var belongs: Seq[GroupProject] = Nil,
                             var metrics: Metrics = null,
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
      KeywordField(name = FieldKeys.FIELD_TAG),
      KeywordField(name = FieldKeys.FIELD_METHOD),
      KeywordField(name = FieldKeys.FIELD_URL_PATH),
      BasicField(name = FieldKeys.FIELD_COUNT, `type` = "long"),
      BasicField(name = FieldKeys.FIELD_PERCENTAGE, `type` = "integer"),
      NestedField(name = FieldKeys.FIELD_BELONGS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_GROUP),
        KeywordField(name = FieldKeys.FIELD_PROJECT),
        BasicField(name = FieldKeys.FIELD_COVERED, `type` = "boolean"),
        BasicField(name = FieldKeys.FIELD_COUNT, `type` = "long"),
      )),
      ObjectField(name = FieldKeys.FIELD_METRICS, fields = Seq(
        BasicField(name = FieldKeys.FIELD_P25, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_P50, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_P75, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_P90, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_P95, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_P99, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_P999, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_MIN, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_AVG, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_MAX, `type` = "integer"),
      )),
    )
  )

  case class GroupProject(group: String, project: String, var covered: Boolean = false, var count: Long = 0)

}
