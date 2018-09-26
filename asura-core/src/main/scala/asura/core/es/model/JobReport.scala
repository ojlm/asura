package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.job.JobExecDesc
import com.sksamuel.elastic4s.mappings._

case class JobReport(
                      val scheduler: String,
                      val group: String,
                      val project: String,
                      val jobId: String,
                      val jobName: String,
                      val `type`: String,
                      val classAlias: String,
                      var startAt: String = null,
                      var endAt: String = null,
                      var elapse: Long = 0L,
                      var result: String = JobExecDesc.STATUS_SUCCESS,
                      var errorMsg: String = StringUtils.EMPTY,
                      val node: String = JobReport.hostname,
                      val data: JobReportData = JobReportData(),
                      val summary: String = StringUtils.EMPTY,
                      val description: String = StringUtils.EMPTY,
                      var creator: String = null,
                      var createdAt: String = null,
                    ) extends BaseIndex {
}

object JobReport extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}job-report"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_SCHEDULER),
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_JOB_ID),
      KeywordField(name = FieldKeys.FIELD_JOB_NAME),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_CLASS_ALIAS),
      BasicField(name = FieldKeys.FIELD_START_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicField(name = FieldKeys.FIELD_END_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicField(name = FieldKeys.FIELD_ELAPSE, `type` = "long"),
      KeywordField(name = FieldKeys.FIELD_RESULT),
      TextField(name = FieldKeys.FIELD_ERROR_MSG, analysis = EsConfig.IK_ANALYZER),
      KeywordField(name = FieldKeys.FIELD_NODE),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
    )
  )

  val hostname = try {
    import scala.sys.process._
    "hostname".!!.trim
  } catch {
    case _: Throwable => "Unknown"
  }

  val TYPE_QUARTZ = "quartz"
  val TYPE_CI = "ci"
  val TYPE_TEST = "test"
  val TYPE_MANUAL = "manual"
}
