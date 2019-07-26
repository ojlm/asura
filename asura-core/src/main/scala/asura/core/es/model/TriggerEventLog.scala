package asura.core.es.model

import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.EsConfig
import asura.core.es.model.TriggerEventLog.ExtData
import asura.core.job.JobExecDesc
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition, ObjectField}

case class TriggerEventLog(
                            group: String,
                            project: String,
                            env: String,
                            author: String,
                            service: String,
                            `type`: String,
                            timestamp: String,
                            result: String,
                            triggerId: String = StringUtils.EMPTY,
                            targetType: String = StringUtils.EMPTY,
                            targetId: String = StringUtils.EMPTY,
                            reportId: String = StringUtils.EMPTY,
                            createdAt: String = DateUtils.nowDateTime,
                            ext: ExtData = null,
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
      KeywordField(name = FieldKeys.FIELD_RESULT),
      KeywordField(name = FieldKeys.FIELD_TRIGGER_ID),
      KeywordField(name = FieldKeys.FIELD_TARGET_TYPE),
      KeywordField(name = FieldKeys.FIELD_TARGET_ID),
      KeywordField(name = FieldKeys.FIELD_REPORT_ID),
      BasicField(name = FieldKeys.FIELD_TIMESTAMP, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicField(name = FieldKeys.FIELD_CREATED_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
      ObjectField(name = FieldKeys.FIELD_EXT, dynamic = Option("false")),
    )
  )

  /** no triggers match */
  val RESULT_MISS = "miss"
  /** is debounced */
  val RESULT_DEBOUNCE = "debounce"
  /** readiness check failed */
  val RESULT_ILL = "ill"
  /** report success */
  val RESULT_SUCCESS = JobExecDesc.STATUS_SUCCESS
  /** report fail */
  val RESULT_FAIL = JobExecDesc.STATUS_FAIL

  case class ExtData(errMsg: String)

}
