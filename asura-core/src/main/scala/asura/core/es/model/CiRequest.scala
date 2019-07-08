package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

// TODO
case class CiRequest(
                      val summary: String,
                      val description: String,
                      val group: String,
                      val project: String,
                      val `type`: String,
                      val targetId: String,
                      var creator: String = null,
                      var createdAt: String = null,
                      var updatedAt: String = null,
                    ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != `type`) {
      m += (FieldKeys.FIELD_TYPE -> `type`)
    }
    if (null != targetId) {
      m += (FieldKeys.FIELD_TARGET_ID -> targetId)
    }
    m.toMap
  }
}

object CiRequest extends IndexSetting {
  override val Index: String = s"${EsConfig.IndexPrefix}ci-request"
  override val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_TARGET_ID),
    )
  )
}
