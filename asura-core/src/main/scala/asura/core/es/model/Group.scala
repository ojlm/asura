package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{KeywordFieldDefinition, MappingDefinition}

import scala.collection.mutable

case class Group(
                  val id: String,
                  val summary: String,
                  val description: String,
                  val avatar: String = null,
                  var creator: String = null,
                  var createdAt: String = null,
                ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    m.toMap
  }
}

object Group extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}group"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_ID),
      KeywordFieldDefinition(name = FieldKeys.FIELD_AVATAR, index = Option("false"))
    )
  )
}
