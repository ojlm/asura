package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.job.JobExecDesc
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class JobReport(
                      val scheduler: String,
                      val group: String,
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

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    m.toMap
  }
}

object JobReport extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}job-report"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_SCHEDULER),
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
      KeywordFieldDefinition(name = FieldKeys.FIELD_JOB_NAME),
      KeywordFieldDefinition(name = FieldKeys.FIELD_TYPE),
      KeywordFieldDefinition(name = FieldKeys.FIELD_CLASS_ALIAS),
      BasicFieldDefinition(name = FieldKeys.FIELD_START_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicFieldDefinition(name = FieldKeys.FIELD_END_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
      BasicFieldDefinition(name = FieldKeys.FIELD_ELAPSE, `type` = "long"),
      KeywordFieldDefinition(name = FieldKeys.FIELD_RESULT),
      TextFieldDefinition(name = FieldKeys.FIELD_ERROR_MSG, analysis = EsConfig.IK_ANALYZER),
      KeywordFieldDefinition(name = FieldKeys.FIELD_NODE),
      ObjectFieldDefinition(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
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
