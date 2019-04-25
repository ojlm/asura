package asura.core.es.model

import asura.core.es.EsConfig
import asura.core.es.model.DubboRequest.{ArgumentList, ParameterType}
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class DubboRequest(
                         val summary: String,
                         val description: String,
                         val group: String,
                         val project: String,
                         val dubboGroup: String,
                         var version: String,
                         val interface: String,
                         val method: String,
                         val parameterTypes: Seq[ParameterType],
                         val args: ArgumentList,
                         val address: String,
                         val port: Int,
                         val zkAddr: String,
                         val zkPort: Int,
                         var path: String,
                         val assert: Map[String, Any],
                         val labels: Seq[LabelRef] = Nil,
                         var creator: String = null,
                         var createdAt: String = null,
                         var updatedAt: String = null,
                       ) extends BaseIndex {

  override def toUpdateScriptParams: (String, Map[String, Any]) = {
    val sb = StringBuilder.newBuilder
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m, sb)
    if (null != dubboGroup) {
      m += (FieldKeys.FIELD_DUBBO_GROUP -> dubboGroup)
      addScriptUpdateItem(sb, FieldKeys.FIELD_DUBBO_GROUP)
    }
    if (null != interface) {
      m += (FieldKeys.FIELD_INTERFACE -> interface)
      addScriptUpdateItem(sb, FieldKeys.FIELD_INTERFACE)
    }
    if (null != method) {
      m += (FieldKeys.FIELD_METHOD -> method)
      addScriptUpdateItem(sb, FieldKeys.FIELD_METHOD)
    }
    if (null != parameterTypes) {
      m += (FieldKeys.FIELD_PARAMETER_TYPES -> JacksonSupport.mapper.convertValue(parameterTypes, classOf[java.util.List[Map[String, Any]]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_PARAMETER_TYPES)
    } else {
      m += (FieldKeys.FIELD_PARAMETER_TYPES -> Nil)
      addScriptUpdateItem(sb, FieldKeys.FIELD_PARAMETER_TYPES)
    }
    if (null != args) {
      m += (FieldKeys.FIELD_ARGS -> JacksonSupport.mapper.convertValue(args, classOf[java.util.Map[String, Any]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_ARGS)
    }
    if (null != address) {
      m += (FieldKeys.FIELD_ADDRESS -> address)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ADDRESS)
    }
    if (port > 0) {
      m += (FieldKeys.FIELD_PORT -> port)
      addScriptUpdateItem(sb, FieldKeys.FIELD_PORT)
    }
    if (null != path) {
      m += (FieldKeys.FIELD_PATH -> path)
      addScriptUpdateItem(sb, FieldKeys.FIELD_PATH)
    }
    if (null != version) {
      m += (FieldKeys.FIELD_VERSION -> version)
      addScriptUpdateItem(sb, FieldKeys.FIELD_VERSION)
    }
    if (null != zkAddr) {
      m += (FieldKeys.FIELD_ZK_ADDR -> zkAddr)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ZK_ADDR)
    }
    if (zkPort > 0) {
      m += (FieldKeys.FIELD_ZK_PORT -> zkPort)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ZK_PORT)
    }
    if (null != assert) {
      m += (FieldKeys.FIELD_ASSERT -> assert)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ASSERT)
    }
    if (null != labels) {
      m += (FieldKeys.FIELD_LABELS -> JacksonSupport.mapper.convertValue(labels, classOf[java.util.List[Map[String, Any]]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_LABELS)
    }
    (sb.toString, m.toMap)
  }
}

object DubboRequest extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}dubbo-request"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_DUBBO_GROUP),
      KeywordField(name = FieldKeys.FIELD_INTERFACE),
      KeywordField(name = FieldKeys.FIELD_METHOD),
      NestedField(name = FieldKeys.FIELD_PARAMETER_TYPES, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_TYPE),
      )),
      ObjectField(name = FieldKeys.FIELD_ARGS, dynamic = Some("false")),
      KeywordField(name = FieldKeys.FIELD_ADDRESS),
      BasicField(name = FieldKeys.FIELD_PORT, `type` = "integer"),
      KeywordField(name = FieldKeys.FIELD_PATH),
      KeywordField(name = FieldKeys.FIELD_VERSION),
      ObjectField(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
      KeywordField(name = FieldKeys.FIELD_ZK_ADDR),
      BasicField(name = FieldKeys.FIELD_ZK_PORT, `type` = "integer"),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
    )
  )

  case class ParameterType(`type`: String)

  case class ArgumentList(args: Seq[Object])

}
