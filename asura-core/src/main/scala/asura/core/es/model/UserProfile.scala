package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{KeywordFieldDefinition, MappingDefinition}

import scala.collection.mutable

case class UserProfile(
                        val username: String,
                        val email: String,
                        val avatar: String = null,
                        val summary: String,
                        val description: String,
                        var creator: String = null,
                        var createdAt: String = null,
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
      KeywordFieldDefinition(name = FieldKeys.FIELD_USERNAME),
      KeywordFieldDefinition(name = FieldKeys.FIELD_EMAIL, index = Option("false")),
      KeywordFieldDefinition(name = FieldKeys.FIELD_AVATAR, index = Option("false"))
    )
  )
}

