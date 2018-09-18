package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

/**
  * there should be only one `Environment` in a whole job/scenario/case runtime
  *
  * @param summary
  * @param description
  * @param group
  * @param project
  * @param protocol
  * @param host
  * @param port
  * @param auth
  * @param namespace for proxy usage
  * @param custom
  * @param creator
  * @param createdAt
  */
case class Environment(
                        val summary: String,
                        val description: String,
                        val group: String,
                        val project: String,
                        @deprecated
                        val protocol: String,
                        @deprecated
                        val host: String,
                        @deprecated
                        val port: Int,
                        val auth: Authorization,
                        val namespace: String = null,
                        val custom: Seq[KeyValueObject] = Nil,
                        var creator: String = null,
                        var createdAt: String = null,
                      ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (StringUtils.isNotEmpty(namespace)) {
      m += (FieldKeys.FIELD_NAMESPACE -> namespace)
    }
    if (null != auth) {
      m += (FieldKeys.FIELD_AUTH -> auth)
    }
    if (null != custom) {
      m += (FieldKeys.FIELD_CUSTOM -> custom)
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
      ObjectField(name = FieldKeys.FIELD_AUTH, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_TYPE),
        ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
      )),
      NestedField(name = FieldKeys.FIELD_CUSTOM, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_KEY),
        KeywordField(name = FieldKeys.FIELD_VALUE),
        BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
      )),
    )
  )
}
