package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class Job(
                val summary: String,
                val description: String,
                val name: String,
                val group: String,
                val scheduler: String,
                val classAlias: String,
                val trigger: Seq[JobTrigger],
                val jobData: JobData,
                var creator: String = null,
                var createdAt: String = null) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != trigger) {
      m += (FieldKeys.FIELD_TRIGGER -> trigger)
    }
    if (null != jobData) {
      m += (FieldKeys.FIELD_JOB_DATA -> jobData)
    }
    m.toMap
  }
}

object Job extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}job"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_NAME),
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
      KeywordFieldDefinition(name = FieldKeys.FIELD_SCHEDULER),
      KeywordFieldDefinition(name = FieldKeys.FIELD_CLASS_ALIAS),
      NestedFieldDefinition(name = FieldKeys.FIELD_TRIGGER, fields = Seq(
        KeywordFieldDefinition(name = FieldKeys.FIELD_NAME),
        KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
        TextFieldDefinition(name = FieldKeys.FIELD_DESCRIPTION, analysis = EsConfig.IK_ANALYZER),
        KeywordFieldDefinition(name = FieldKeys.FIELD_CRON),
        KeywordFieldDefinition(name = FieldKeys.FIELD_TRIGGER_TYPE),
        BasicFieldDefinition(name = FieldKeys.FIELD_START_NOW, `type` = "boolean"),
        BasicFieldDefinition(name = FieldKeys.FIELD_START_DATE, `type` = "date", format = Some(EsConfig.DateFormat)),
        BasicFieldDefinition(name = FieldKeys.FIELD_END_DATE, `type` = "date", format = Some(EsConfig.DateFormat)),
        BasicFieldDefinition(name = FieldKeys.FIELD_REPEAT_COUNT, `type` = "integer"),
        BasicFieldDefinition(name = FieldKeys.FIELD_INTERVAL, `type` = "integer"),
      )),
      ObjectFieldDefinition(name = FieldKeys.FIELD_JOB_DATA, fields = Seq(
        NestedFieldDefinition(name = FieldKeys.FIELD_CS, fields = Seq(
          KeywordFieldDefinition(name = FieldKeys.FIELD_ID),
        )),
        NestedFieldDefinition(name = FieldKeys.FIELD_SCENARIO, fields = Seq(
          KeywordFieldDefinition(name = FieldKeys.FIELD_ID),
        )),
        ObjectFieldDefinition(name = FieldKeys.FIELD_EXT, dynamic = Option("false")),
      )),
    )
  )

  def buildJobKey(scheduler: String, jobGroup: String, jobName: String): String = {
    val sb = StringBuilder.newBuilder
    sb.append(scheduler).append("_").append(jobGroup).append("_").append(jobName)
    sb.toString
  }

  def buildJobKey(job: Job): String = {
    buildJobKey(job.scheduler, job.group, job.name)
  }
}
