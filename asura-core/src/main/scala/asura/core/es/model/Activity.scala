package asura.core.es.model

import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition, ObjectField}

case class Activity(
                     val group: String,
                     val project: String,
                     val user: String,
                     val `type`: String,
                     val targetId: String = StringUtils.EMPTY,
                     val timestamp: String = DateUtils.nowDateTime,
                     val data: Map[String, Any] = null,
                   )

object Activity extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}activity"
  override val shards: Int = 5
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_USER),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_TARGET_ID),
      BasicField(name = FieldKeys.FIELD_TIMESTAMP, `type` = "date", format = Some(EsConfig.DateFormat)),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
    )
  )

  // types
  val TYPE_NEW_USER = "new-user"


}


