package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class ReportNotify(
                         val summary: String = null,
                         val description: String = null,
                         val group: String,
                         val project: String,
                         val jobId: String,
                         val subscriber: String,
                         val `type`: String,
                         val data: Map[String, Any] = null,
                         val trigger: String = ReportNotify.TRIGGER_ALL,
                         val enabled: Boolean = true,
                         var creator: String = null,
                         var createdAt: String = null,
                       ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    if (Option(enabled).isDefined) {
      m += (FieldKeys.FIELD_ENABLED -> enabled)
    }
    if (StringUtils.isNotEmpty(trigger)) {
      m += (FieldKeys.FIELD_TRIGGER -> trigger)
    }
    m.toMap
  }
}

object ReportNotify extends IndexSetting {

  val TRIGGER_ALL = "all"
  val TRIGGER_FAIL = "fail"
  val TRIGGER_SUCCESS = "success"

  val Index: String = s"${EsConfig.IndexPrefix}job-notify"
  override val shards: Int = 3
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
      KeywordFieldDefinition(name = FieldKeys.FIELD_PROJECT),
      KeywordFieldDefinition(name = FieldKeys.FIELD_JOB_ID),
      KeywordFieldDefinition(name = FieldKeys.FIELD_SUBSCRIBER),
      KeywordFieldDefinition(name = FieldKeys.FIELD_TYPE),
      KeywordFieldDefinition(name = FieldKeys.FIELD_TRIGGER),
      BasicFieldDefinition(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
      ObjectFieldDefinition(name = FieldKeys.FIELD_DATA, dynamic = Some("false"))
    )
  )
}
