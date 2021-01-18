package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

case class LogEntry(
                     group: String,
                     project: String,
                     taskId: String,
                     reportId: String,
                     `type`: String,
                     hostname: String,
                     pid: String, // process or package name
                     var level: String = null,
                     var source: String = null, // network, javascript, class
                     var text: String = null,
                     var timestamp: Long = 0L,
                     var data: java.util.Map[Object, Object] = null,
                   )

object LogEntry extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}log-entry"
  override val shards: Int = 5
  override val replicas: Int = 0
  val mappings: MappingDefinition = Es6MappingDefinition(
    Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_TASK_ID),
      KeywordField(name = FieldKeys.FIELD_REPORT_ID),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_HOSTNAME),
      KeywordField(name = FieldKeys.FIELD_PID),
      KeywordField(name = FieldKeys.FIELD_LEVEL),
      KeywordField(name = FieldKeys.FIELD_SOURCE),
      TextField(name = FieldKeys.FIELD_TEXT, analysis = EsConfig.IK_ANALYZER),
      BasicField(name = FieldKeys.FIELD_TIMESTAMP, `type` = "date", format = Some(EsConfig.DateFormat)),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
    )
  )

  val TYPE_CONSOLE = "console"
  val TYPE_MONKEY = "monkey"

}
