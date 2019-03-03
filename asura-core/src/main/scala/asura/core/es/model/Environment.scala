package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

/**
  * there should be only one `Environment` in a whole job/scenario/case runtime
  */
case class Environment(
                        val summary: String,
                        val description: String,
                        val group: String,
                        val project: String,
                        val auth: Seq[Authorization],
                        val namespace: String = null,
                        val enableProxy: Boolean = false,
                        val server: String = null,
                        val custom: Seq[KeyValueObject] = Nil,
                        val headers: Seq[KeyValueObject] = Nil,
                        var creator: String = null,
                        var createdAt: String = null,
                        var updatedAt: String = null,
                      ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != namespace) {
      m += (FieldKeys.FIELD_NAMESPACE -> namespace)
    }
    if (Option(enableProxy).isDefined) {
      m += (FieldKeys.FIELD_ENABLE_PROXY -> enableProxy)
    }
    if (null != server) {
      m += (FieldKeys.FIELD_SERVER -> server)
    }
    if (null != auth) {
      m += (FieldKeys.FIELD_AUTH -> auth)
    }
    if (null != custom) {
      m += (FieldKeys.FIELD_CUSTOM -> custom)
    }
    if (null != headers) {
      m += (FieldKeys.FIELD_HEADERS -> headers)
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
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_PROTOCOL),
      KeywordField(name = FieldKeys.FIELD_HOST),
      BasicField(name = FieldKeys.FIELD_PORT, `type` = "integer"),
      KeywordField(name = FieldKeys.FIELD_NAMESPACE),
      BasicField(name = FieldKeys.FIELD_ENABLE_PROXY, `type` = "boolean"),
      KeywordField(name = FieldKeys.FIELD_SERVER),
      NestedField(name = FieldKeys.FIELD_AUTH, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_TYPE),
        ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
      )),
      NestedField(name = FieldKeys.FIELD_CUSTOM, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_KEY),
        KeywordField(name = FieldKeys.FIELD_VALUE),
        BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
      )),
      NestedField(name = FieldKeys.FIELD_HEADERS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_KEY),
        KeywordField(name = FieldKeys.FIELD_VALUE),
        BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
      )),
    )
  )
}
