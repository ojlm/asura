package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.es.model.SqlRequest.SqlRequestBody
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class SqlRequest(
                       val summary: String,
                       val description: String,
                       val group: String,
                       val project: String,
                       val request: SqlRequestBody,
                       val assert: Map[String, Any],
                       val env: String = StringUtils.EMPTY,
                       val labels: Seq[LabelRef] = Nil,
                       val generator: RequestGenerator = RequestGenerator(),
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
    if (null != generator) {
      m += (FieldKeys.FIELD_GENERATOR -> JacksonSupport.mapper.convertValue(generator, classOf[java.util.Map[String, Any]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_GENERATOR)
    }
    (sb.toString, m.toMap)
  }
}

object SqlRequest extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}sql-request"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_ENV),
      ObjectField(
        name = FieldKeys.FIELD_REQUEST,
        fields = Seq(
          KeywordField(name = FieldKeys.FIELD_HOST),
          BasicField(name = FieldKeys.FIELD_PORT, `type` = "integer"),
          KeywordField(name = FieldKeys.FIELD_USERNAME),
          KeywordField(name = FieldKeys.FIELD_PASSWORD),
          KeywordField(name = FieldKeys.FIELD_ENCRYPTED_PASS, index = Some("false")),
          KeywordField(name = FieldKeys.FIELD_DATABASE),
          KeywordField(name = FieldKeys.FIELD_TABLE),
          TextField(name = FieldKeys.FIELD_SQL, analysis = EsConfig.IK_ANALYZER),
        )
      ),
      ObjectField(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
      ObjectField(name = FieldKeys.FIELD_GENERATOR, fields = Seq(
        TextField(name = FieldKeys.FIELD_SCRIPT, index = Option("false")),
        NestedField(name = FieldKeys.FIELD_LIST, dynamic = Some("false")),
        BasicField(name = FieldKeys.FIELD_COUNT, `type` = "integer"),
      )),
    )
  )

  case class SqlRequestBody(
                             val host: String,
                             val port: Int,
                             val username: String,
                             var password: String,
                             var encryptedPass: String,
                             val database: String,
                             var table: String,
                             val sql: String,
                           )

}
