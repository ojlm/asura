package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{KeywordField, MappingDefinition}

import scala.collection.mutable

case class UserProfile(
                        val username: String,
                        val nickname: String = null,
                        val email: String = null,
                        val avatar: String = null,
                        val summary: String = null,
                        val description: String = null,
                        var creator: String = null,
                        var createdAt: String = null,
                        var updatedAt: String = null,
                      ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != email) {
      m += (FieldKeys.FIELD_EMAIL -> email)
    }
    if (null != avatar) {
      m += (FieldKeys.FIELD_AVATAR -> avatar)
    }
    if (null != nickname) {
      m += (FieldKeys.FIELD_NICKNAME -> nickname)
    }
    m.toMap
  }
}

object UserProfile extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}user-profile"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_USERNAME),
      KeywordField(name = FieldKeys.FIELD_NICKNAME),
      KeywordField(name = FieldKeys.FIELD_EMAIL),
      KeywordField(name = FieldKeys.FIELD_AVATAR, index = Option("false"))
    )
  )
}

