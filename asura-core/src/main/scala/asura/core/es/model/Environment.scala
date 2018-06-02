package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicFieldDefinition, KeywordFieldDefinition, MappingDefinition, ObjectFieldDefinition}

import scala.collection.mutable

case class Environment(
                        val summary: String,
                        val description: String,
                        val group: String,
                        val project: String,
                        val protocol: String,
                        val host: String,
                        val port: Int,
                        val auth: Authorization,
                        var creator: String = null,
                        var createdAt: String = null,
                      ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (StringUtils.isNotEmpty(protocol)) {
      m += (FieldKeys.FIELD_PROTOCOL -> protocol)
    }
    if (StringUtils.isNotEmpty(host)) {
      m += (FieldKeys.FIELD_HOST -> host)
    }
    if (Option(port).isDefined) {
      m += (FieldKeys.FIELD_PORT -> port)
    }
    if (null != auth) {
      m += (FieldKeys.FIELD_AUTH -> auth)
    }
    m.toMap
  }
}

object Environment extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}env"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
      KeywordFieldDefinition(name = FieldKeys.FIELD_PROJECT),
      KeywordFieldDefinition(name = FieldKeys.FIELD_PROTOCOL),
      KeywordFieldDefinition(name = FieldKeys.FIELD_HOST),
      BasicFieldDefinition(name = FieldKeys.FIELD_PORT, `type` = "integer"),
      ObjectFieldDefinition(name = FieldKeys.FIELD_AUTH, fields = Seq(
        KeywordFieldDefinition(name = FieldKeys.FIELD_TYPE),
        ObjectFieldDefinition(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
      ))
    )
  )
}
