package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class Job(
                val summary: String,
                val description: String,
                val name: String,
                val group: String,
                val project: String,
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
      KeywordField(name = FieldKeys.FIELD_NAME),
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_SCHEDULER),
      KeywordField(name = FieldKeys.FIELD_CLASS_ALIAS),
      NestedField(name = FieldKeys.FIELD_TRIGGER, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
        KeywordField(name = FieldKeys.FIELD_GROUP),
        KeywordField(name = FieldKeys.FIELD_PROJECT),
        TextField(name = FieldKeys.FIELD_DESCRIPTION, analysis = EsConfig.IK_ANALYZER),
        KeywordField(name = FieldKeys.FIELD_CRON),
        KeywordField(name = FieldKeys.FIELD_TRIGGER_TYPE),
        BasicField(name = FieldKeys.FIELD_START_NOW, `type` = "boolean"),
        BasicField(name = FieldKeys.FIELD_START_DATE, `type` = "date", format = Some(EsConfig.DateFormat)),
        BasicField(name = FieldKeys.FIELD_END_DATE, `type` = "date", format = Some(EsConfig.DateFormat)),
        BasicField(name = FieldKeys.FIELD_REPEAT_COUNT, `type` = "integer"),
        BasicField(name = FieldKeys.FIELD_INTERVAL, `type` = "integer"),
      )),
      ObjectField(name = FieldKeys.FIELD_JOB_DATA, fields = Seq(
        NestedField(name = FieldKeys.FIELD_CS, fields = Seq(
          KeywordField(name = FieldKeys.FIELD_ID),
        )),
        NestedField(name = FieldKeys.FIELD_SCENARIO, fields = Seq(
          KeywordField(name = FieldKeys.FIELD_ID),
        )),
        ObjectField(name = FieldKeys.FIELD_EXT, dynamic = Option("false")),
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
