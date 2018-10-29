package asura.core.es.model

import asura.common.util.DateUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition, ObjectField}

case class Activity(
                     val group: String,
                     val project: String,
                     val user: String,
                     val `type`: String,
                     val targetId: String,
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
  val TYPE_NEW_CASE = "new-case"
  val TYPE_TEST_CASE = "test-case"
  val TYPE_UPDATE_CASE = "update-case"
  val TYPE_NEW_GROUP = "new-group"
  val TYPE_DELETE_GROUP = "delete-group"
  val TYPE_NEW_PROJECT = "new-project"
  val TYPE_DELETE_PROJECT = "delete-project"
  val TYPE_NEW_SCENARIO = "new-scenario"
  val TYPE_TEST_SCENARIO = "test-scenario"
  val TYPE_NEW_JOB = "new-job"
  val TYPE_TEST_JOB = "test-job"
}


