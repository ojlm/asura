package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.requests.mappings.{KeywordField, MappingDefinition, ObjectField}
import net.minidev.json.annotate.JsonIgnore

import scala.collection.mutable

case class Permissions(
                        summary: String = null,
                        description: String = null,
                        var group: String,
                        var project: String,
                        var `type`: String,
                        username: String,
                        role: String,
                        data: Map[String, Any] = null,
                        var creator: String = null,
                        var createdAt: String = null,
                        var updatedAt: String = null,
                      ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (Permissions.isValidRole(role)) {
      m += (FieldKeys.FIELD_ROLE -> role)
    }
    if (null != data) {
      m += (FieldKeys.FIELD_DATA -> data)
    }
    m.toMap
  }

  @JsonIgnore
  def isValidRole(): Boolean = Permissions.isValidRole(role)
}

object Permissions extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}permissions"
  override val shards: Int = 5
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_USERNAME),
      KeywordField(name = FieldKeys.FIELD_ROLE),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
    )
  )

  val TYPE_GROUP = "group"
  val TYPE_PROJECT = "project"

  // roles
  val ROLE_OWNER = "owner"
  val ROLE_MAINTAINER = "maintainer"
  val ROLE_DEVELOPER = "developer"
  val ROLE_REPORTER = "reporter"
  val ROLE_GUEST = "guest"

  def isValidRole(role: String): Boolean = {
    role match {
      case ROLE_OWNER | ROLE_MAINTAINER | ROLE_DEVELOPER | ROLE_REPORTER | ROLE_GUEST => true
      case _ => false
    }
  }
}
