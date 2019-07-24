package asura.core.es.model

import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition}

case class TriggerEventLog(
                            group: String,
                            project: String,
                            env: String,
                            author: String,
                            service: String,
                            `type`: String,
                            timestamp: String,
                            triggerId: String = StringUtils.EMPTY,
                            jobId: String = StringUtils.EMPTY,
                            reportId: String = StringUtils.EMPTY,
                            createdAt: String = DateUtils.nowDateTime
                          )

object TriggerEventLog extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}trigger-events"
  override val shards: Int = 5
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_ENV),
      KeywordField(name = FieldKeys.FIELD_AUTHOR),
      KeywordField(name = FieldKeys.FIELD_SERVICE),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_TRIGGER_ID),
      KeywordField(name = FieldKeys.FIELD_JOB_ID),
      KeywordField(name = FieldKeys.FIELD_REPORT_ID),
      BasicField(name = FieldKeys.FIELD_TIMESTAMP, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicField(name = FieldKeys.FIELD_CREATED_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
    )
  )
}
