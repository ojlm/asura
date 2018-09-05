package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{KeywordFieldDefinition, MappingDefinition}

import scala.collection.mutable

/** group and id should composite a unique key */
case class Project(
                    val id: String,
                    val summary: String,
                    val description: String,
                    val group: String,
                    val openapi: String = null,
                    val avatar: String = null,
                    var creator: String = null,
                    var createdAt: String = null,
                  ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (StringUtils.isNotEmpty(avatar)) {
      m += (FieldKeys.FIELD_AVATAR -> avatar)
    }
    if (null != openapi) {
      m += (FieldKeys.FIELD_OPENAPI -> openapi)
    }
    m.toMap
  }

  def generateDocId() = Project.generateDocId(group, id)
}

object Project extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}project"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_ID, copyTo = Seq(FieldKeys.FIELD__TEXT)),
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
      KeywordFieldDefinition(name = FieldKeys.FIELD_OPENAPI, index = Option("false")),
      KeywordFieldDefinition(name = FieldKeys.FIELD_AVATAR, index = Option("false")),
    )
  )

  def generateDocId(group: String, projectId: String) = s"${group}_${projectId}"
}
