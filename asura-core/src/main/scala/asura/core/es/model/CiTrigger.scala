package asura.core.es.model

import asura.core.es.EsConfig
import asura.core.es.model.CiTrigger.ReadinessCheck
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class CiTrigger(
                      summary: String,
                      description: String,
                      group: String,
                      project: String,
                      targetType: String,
                      targetId: String,
                      env: String,
                      service: String,
                      enabled: Boolean = false,
                      debounce: Long = 1000L,
                      readiness: ReadinessCheck = null,
                      var creator: String = null,
                      var createdAt: String = null,
                      var updatedAt: String = null,
                    ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != targetType) {
      m += (FieldKeys.FIELD_TARGET_TYPE -> targetType)
    }
    if (null != targetId) {
      m += (FieldKeys.FIELD_TARGET_ID -> targetId)
    }
    if (null != env) {
      m += (FieldKeys.FIELD_ENV -> env)
    }
    if (null != service) {
      m += (FieldKeys.FIELD_SERVICE -> service)
    }
    m += (FieldKeys.FIELD_DEBOUNCE -> debounce)
    m += (FieldKeys.FIELD_ENABLED -> enabled)
    if (null != readiness) {
      m += (FieldKeys.FIELD_READINESS -> readiness)
    }
    m.toMap
  }
}

object CiTrigger extends IndexSetting {

  override val Index: String = s"${EsConfig.IndexPrefix}ci-trigger"
  override val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_TARGET_TYPE),
      KeywordField(name = FieldKeys.FIELD_TARGET_ID),
      KeywordField(name = FieldKeys.FIELD_ENV),
      KeywordField(name = FieldKeys.FIELD_SERVICE),
      BasicField(name = FieldKeys.FIELD_DEBOUNCE, `type` = "long"),
      BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
      ObjectField(name = FieldKeys.FIELD_READINESS, dynamic = Option("false")),
    )
  )

  case class ReadinessCheck(
                             enabled: Boolean,
                             targetType: String, // same with asura.core.es.model.ScenarioStep
                             targetId: String,
                             delay: Int, // s
                             interval: Int, // s
                             timeout: Int, // s
                             retries: Int,
                           )

}
