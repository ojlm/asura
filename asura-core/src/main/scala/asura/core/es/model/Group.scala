package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.requests.mappings.{KeywordField, MappingDefinition}

import scala.collection.mutable

case class Group(
                  var id: String,
                  summary: String,
                  description: String,
                  avatar: String = null,
                  var creator: String = null,
                  var createdAt: String = null,
                  var updatedAt: String = null,
                ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (StringUtils.isNotEmpty(avatar)) {
      m += (FieldKeys.FIELD_AVATAR -> avatar)
    }
    m.toMap
  }
}

object Group extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}group"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_ID, copyTo = Seq(FieldKeys.FIELD__TEXT)),
      KeywordField(name = FieldKeys.FIELD_AVATAR, index = Option("false"))
    )
  )
}
