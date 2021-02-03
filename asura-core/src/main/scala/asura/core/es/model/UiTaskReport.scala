package asura.core.es.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import asura.common.util.{HostUtils, StringUtils}
import asura.core.es.EsConfig
import asura.core.job.JobExecDesc
import com.fasterxml.jackson.annotation.JsonIgnore
import com.sksamuel.elastic4s.mappings._

case class UiTaskReport(
                         summary: String,
                         description: String,
                         group: String,
                         project: String,
                         taskId: String,
                         `type`: String,
                         var params: Map[String, Any],
                         startAt: Long = 0L,
                         var endAt: Long = 0L,
                         var elapse: Long = 0L,
                         var result: String = JobExecDesc.STATUS_SUCCESS,
                         var errorMsg: String = StringUtils.EMPTY,
                         node: String = HostUtils.hostname,
                         day: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern(EsConfig.INDEX_DATE_TIME_PATTERN)),
                         var data: UiTaskReportData = UiTaskReportData(),
                         var creator: String,
                         var createdAt: String,
                         var updatedAt: String,
                       ) extends BaseIndex {

  @JsonIgnore
  def isSuccessful(): Boolean = {
    JobExecDesc.STATUS_SUCCESS == result
  }

}

object UiTaskReport extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}ui-task-report"
  val mappings: MappingDefinition = Es6MappingDefinition(
    BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_TASK_ID),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      BasicField(name = FieldKeys.FIELD_START_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicField(name = FieldKeys.FIELD_END_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicField(name = FieldKeys.FIELD_ELAPSE, `type` = "long"),
      KeywordField(name = FieldKeys.FIELD_RESULT),
      TextField(name = FieldKeys.FIELD_ERROR_MSG, analysis = EsConfig.IK_ANALYZER),
      KeywordField(name = FieldKeys.FIELD_NODE),
      KeywordField(name = FieldKeys.FIELD_DAY),
      ObjectField(name = FieldKeys.FIELD_PARAMS, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
    )
  )

}

