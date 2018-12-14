package asura.core.es.model

import asura.core.es.EsConfig
import asura.core.es.model.RestApiOnlineLog.GroupProject
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition, NestedField}

case class RestApiOnlineLog(
                             val domain: String,
                             val method: String,
                             val urlPath: String,
                             val count: Long,
                             val percentage: Int, // like 44.22 but saved 4422
                             var belongs: Seq[GroupProject] = Nil,
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
      BasicField(name = FieldKeys.FIELD_COUNT, `type` = "long"),
      BasicField(name = FieldKeys.FIELD_PERCENTAGE, `type` = "integer"),
      NestedField(name = FieldKeys.FIELD_BELONGS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_GROUP),
        KeywordField(name = FieldKeys.FIELD_PROJECT),
        BasicField(name = FieldKeys.FIELD_COVERED, `type` = "boolean"),
        BasicField(name = FieldKeys.FIELD_COUNT, `type` = "long"),
      )),
    )
  )

  case class GroupProject(group: String, project: String, var covered: Boolean = false, var count: Long = 0)

}
