package asura.core.es.model

import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class JobNotify(
                      val summary: String = null,
                      val description: String = null,
                      val group: String,
                      val project: String,
                      var jobId: String,
                      val subscriber: String,
                      val `type`: String,
                      val data: Map[String, Any] = Map.empty,
                      var trigger: String = JobNotify.TRIGGER_ALL,
                      var enabled: Boolean = true,
                      var creator: String = null,
                      var createdAt: String = null,
                      var updatedAt: String = null,
                    ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    m += (FieldKeys.FIELD_UPDATED_AT -> DateUtils.nowDateTime)
    if (null != `type`) {
      m += (FieldKeys.FIELD_TYPE -> `type`)
    }
    if (null != subscriber) {
      m += (FieldKeys.FIELD_SUBSCRIBER -> subscriber)
    }
    if (Option(enabled).isDefined) {
      m += (FieldKeys.FIELD_ENABLED -> enabled)
    }
    if (StringUtils.isNotEmpty(trigger)) {
      m += (FieldKeys.FIELD_TRIGGER -> trigger)
    }
    if (null != data) {
      m += (FieldKeys.FIELD_DATA -> data)
    }
    m.toMap
  }
}

object JobNotify extends IndexSetting {

  val TRIGGER_ALL = "all"
  val TRIGGER_FAIL = "fail"
  val TRIGGER_SUCCESS = "success"

  val Index: String = s"${EsConfig.IndexPrefix}job-notify"
  override val shards: Int = 3
  override val replicas: Int = 1
  val mappings: MappingDefinition = Es6MappingDefinition(
    BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_JOB_ID),
      KeywordField(name = FieldKeys.FIELD_SUBSCRIBER),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_TRIGGER),
      BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false"))
    )
  )
}
