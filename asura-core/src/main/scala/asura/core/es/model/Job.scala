package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class Job(
                val summary: String,
                val description: String,
                val group: String,
                val project: String,
                val scheduler: String,
                val classAlias: String,
                val trigger: Seq[JobTrigger],
                val jobData: JobData,
                val env: String = StringUtils.EMPTY,
                val imports: Seq[VariablesImportItem] = Nil,
                var creator: String = null,
                var createdAt: String = null,
                var updatedAt: String = null,
              ) extends BaseIndex {

  override def toUpdateScriptParams: (String, Map[String, Any]) = {
    val sb = StringBuilder.newBuilder
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m, sb)
    if (null != env) {
      m += (FieldKeys.FIELD_ENV -> env)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ENV)
    }
    addScriptUpdateItem(sb, FieldKeys.FIELD_TRIGGER)
    if (null != trigger) {
      m += (FieldKeys.FIELD_TRIGGER -> JacksonSupport.mapper.convertValue(trigger, classOf[java.util.List[Map[String, Any]]]))
    } else {
      m += (FieldKeys.FIELD_TRIGGER -> Nil)
    }
    addScriptUpdateItem(sb, FieldKeys.FIELD_IMPORTS)
    if (null != imports) {
      m += (FieldKeys.FIELD_IMPORTS -> JacksonSupport.mapper.convertValue(imports, classOf[java.util.List[Map[String, Any]]]))
    } else {
      m += (FieldKeys.FIELD_IMPORTS -> Nil)
    }
    addScriptUpdateItem(sb, FieldKeys.FIELD_JOB_DATA)
    if (null != jobData) {
      m += (FieldKeys.FIELD_JOB_DATA -> jobDataToMap())
    } else {
      m += (FieldKeys.FIELD_JOB_DATA -> Map.empty)
    }
    (sb.toString, m.toMap)
  }

  private def jobDataToMap(): Map[String, Object] = {
    val cs = if (null != jobData.cs) {
      JacksonSupport.mapper.convertValue(jobData.cs, classOf[java.util.List[Map[String, Any]]])
    } else {
      Nil
    }
    val scenario = if (null != jobData.scenario) {
      JacksonSupport.mapper.convertValue(jobData.scenario, classOf[java.util.List[Map[String, Any]]])
    } else {
      Nil
    }
    val ext = if (null != jobData.ext) {
      JacksonSupport.mapper.convertValue(jobData.ext, classOf[Map[String, Any]])
    } else {
      null
    }
    Map(FieldKeys.FIELD_CS -> cs, FieldKeys.FIELD_SCENARIO -> scenario, FieldKeys.FIELD_EXT -> ext)
  }
}

object Job extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}job"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
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
      KeywordField(name = FieldKeys.FIELD_ENV),
      ObjectField(name = FieldKeys.FIELD_IMPORTS, dynamic = Some("false")),
    )
  )
}
