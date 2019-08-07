package asura.core.es.model

import asura.common.util.{JsonUtils, StringUtils}
import asura.core.es.EsConfig
import asura.core.es.model.DubboRequest.DubboRequestBody
import asura.core.es.model.Label.LabelRef
import asura.core.util.JacksonSupport
import asura.dubbo.GenericRequest
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class DubboRequest(
                         val summary: String,
                         val description: String,
                         val group: String,
                         val project: String,
                         val request: DubboRequestBody,
                         val assert: Map[String, Any],
                         val env: String = StringUtils.EMPTY,
                         val labels: Seq[LabelRef] = Nil,
                         val generator: RequestGenerator = RequestGenerator(),
                         val exports: Seq[VariablesExportItem] = Nil,
                         var creator: String = null,
                         var createdAt: String = null,
                         var updatedAt: String = null,
                       ) extends BaseIndex {

  override def toUpdateScriptParams: (String, Map[String, Any]) = {
    val sb = StringBuilder.newBuilder
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m, sb)
    if (null != env) {
      m += (FieldKeys.FIELD_ENV -> env)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ENV)
    }
    if (null != request) {
      m += (FieldKeys.FIELD_REQUEST -> JacksonSupport.mapper.convertValue(request, classOf[java.util.Map[String, Any]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_REQUEST)
    }
    if (null != assert) {
      m += (FieldKeys.FIELD_ASSERT -> assert)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ASSERT)
    }
    if (null != labels) {
      m += (FieldKeys.FIELD_LABELS -> JacksonSupport.mapper.convertValue(labels, classOf[java.util.List[Map[String, Any]]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_LABELS)
    }
    if (null != exports) {
      m += (FieldKeys.FIELD_EXPORTS -> JacksonSupport.mapper.convertValue(exports, classOf[java.util.List[Map[String, Any]]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_EXPORTS)
    }
    if (null != generator) {
      m += (FieldKeys.FIELD_GENERATOR -> JacksonSupport.mapper.convertValue(generator, classOf[java.util.Map[String, Any]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_GENERATOR)
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
      KeywordField(name = FieldKeys.FIELD_ENV),
      ObjectField(
        name = FieldKeys.FIELD_REQUEST,
        fields = Seq(
          KeywordField(name = FieldKeys.FIELD_DUBBO_GROUP),
          KeywordField(name = FieldKeys.FIELD_INTERFACE),
          KeywordField(name = FieldKeys.FIELD_METHOD),
          NestedField(name = FieldKeys.FIELD_PARAMETER_TYPES, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_TYPE),
          )),
          TextField(name = FieldKeys.FIELD_ARGS, analysis = EsConfig.IK_ANALYZER),
          KeywordField(name = FieldKeys.FIELD_ADDRESS),
          BasicField(name = FieldKeys.FIELD_PORT, `type` = "integer"),
          KeywordField(name = FieldKeys.FIELD_VERSION),
          ObjectField(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
          TextField(name = FieldKeys.FIELD_ZK_CONNECT_STRING, analysis = EsConfig.IK_ANALYZER),
          KeywordField(name = FieldKeys.FIELD_ZK_USERNAME),
          KeywordField(name = FieldKeys.FIELD_ZK_PASSWORD),
          KeywordField(name = FieldKeys.FIELD_PATH),
          BasicField(name = FieldKeys.FIELD_ENABLE_LB, `type` = "boolean"),
          KeywordField(name = FieldKeys.FIELD_LB_ALGORITHM),
        )
      ),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
      ObjectField(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
      ObjectField(name = FieldKeys.FIELD_GENERATOR, fields = Seq(
        TextField(name = FieldKeys.FIELD_SCRIPT, index = Option("false")),
        NestedField(name = FieldKeys.FIELD_LIST, dynamic = Some("false")),
        BasicField(name = FieldKeys.FIELD_COUNT, `type` = "integer"),
      )),
      NestedField(name = FieldKeys.FIELD_EXPORTS, dynamic = Some("false")),
    )
  )

  case class ParameterType(`type`: String)

  case class DubboRequestBody(
                               val dubboGroup: String,
                               var version: String,
                               val interface: String,
                               val method: String,
                               val parameterTypes: Seq[ParameterType],
                               val args: String,
                               val address: String,
                               val port: Int,
                               val zkConnectString: String,
                               val zkUsername: String,
                               val zkPassword: String,
                               var path: String,
                               val enableLb: Boolean = false,
                               val lbAlgorithm: String = StringUtils.EMPTY,
                             ) {

    def toDubboGenericRequest(): GenericRequest = {
      val parameterTypes = if (null != this.parameterTypes && this.parameterTypes.nonEmpty) {
        this.parameterTypes.map(_.`type`).toArray
      } else {
        null
      }
      val args = if (StringUtils.isNotEmpty(this.args)) {
        JsonUtils.parse(this.args, classOf[Array[Object]])
      } else {
        null
      }
      GenericRequest(
        dubboGroup = dubboGroup,
        interface = interface,
        method = method,
        parameterTypes = parameterTypes,
        args = args,
        address = address,
        port = port,
        version = version
      )
    }
  }

  object LoadBalanceAlgorithms {
    val RANDOM = "random"
  }

}
