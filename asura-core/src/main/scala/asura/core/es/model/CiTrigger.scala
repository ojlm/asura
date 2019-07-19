package asura.core.es.model

import asura.core.es.EsConfig
import asura.core.es.model.CiTrigger.ReadinessCheck
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class CiTrigger(
                      val summary: String,
                      val description: String,
                      val group: String,
                      val project: String,
                      val targetType: String,
                      val targetId: String,
                      val readiness: ReadinessCheck = null,
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
      ObjectField(name = FieldKeys.FIELD_READINESS, dynamic = Option("false")),
    )
  )

  case class ReadinessCheck(
                             val targetType: String,
                             val targetId: String,
                             val delay: Int,
                             val interval: Int,
                             val timeout: Int,
                             val retries: Int,
                           )

}
