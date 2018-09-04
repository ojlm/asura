package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings.{BasicFieldDefinition, _}

import scala.collection.mutable

/**
  * is useProxy is `true`, then use `namespace` in env first then can fall back to `namespace`
  */
case class Case(
                 val summary: String,
                 val description: String,
                 val group: String,
                 val project: String,
                 val request: Request,
                 val assert: Map[String, Any],
                 val namespace: String = null,
                 val useProxy: Boolean = false,
                 val env: String = StringUtils.EMPTY,
                 @deprecated("如果 env 不是自认为使用env")
                 val useEnv: Boolean = false,
                 val labels: Seq[LabelRef] = Nil,
                 var creator: String = null,
                 var createdAt: String = null,
               ) extends BaseIndex {

  override def toUpdateScriptParams: (String, Map[String, Any]) = {
    val sb = StringBuilder.newBuilder
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m, sb)
    if (StringUtils.isNotEmpty(env)) {
      m += (FieldKeys.FIELD_ENV -> env)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ENV)
    }
    if (null != request) {
      m += (FieldKeys.FIELD_REQUEST -> JacksonSupport.mapper.convertValue(request, classOf[java.util.Map[String, Any]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_REQUEST)
    }
    if (null != labels) {
      m += (FieldKeys.FIELD_LABELS -> labels)
      addScriptUpdateItem(sb, FieldKeys.FIELD_LABELS)
    }
    if (null != assert) {
      m += (FieldKeys.FIELD_ASSERT -> assert)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ASSERT)
    }
    if (null != namespace) {
      m += (FieldKeys.FIELD_NAMESPACE -> namespace)
      addScriptUpdateItem(sb, FieldKeys.FIELD_NAMESPACE)
    }
    if (Option(useProxy).isDefined) {
      m += (FieldKeys.FIELD_USE_PROXY -> useProxy)
      addScriptUpdateItem(sb, FieldKeys.FIELD_USE_PROXY)
    }
    (sb.toString, m.toMap)
  }
}

object Case extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}case"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
      KeywordFieldDefinition(name = FieldKeys.FIELD_PROJECT),
      KeywordFieldDefinition(name = FieldKeys.FIELD_ENV),
      KeywordFieldDefinition(name = FieldKeys.FIELD_NAMESPACE),
      BasicFieldDefinition(name = FieldKeys.FIELD_USE_PROXY, `type` = "boolean"),
      NestedFieldDefinition(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordFieldDefinition(name = FieldKeys.FIELD_NAME),
      )),
      ObjectFieldDefinition(
        name = FieldKeys.FIELD_REQUEST,
        fields = Seq(
          KeywordFieldDefinition(name = FieldKeys.FIELD_PROTOCOL),
          KeywordFieldDefinition(name = FieldKeys.FIELD_HOST),
          KeywordFieldDefinition(name = FieldKeys.FIELD_RAW_URL, index = Option("false")),
          KeywordFieldDefinition(name = FieldKeys.FIELD_URL_PATH),
          BasicFieldDefinition(name = FieldKeys.FIELD_PORT, `type` = "integer"),
          ObjectFieldDefinition(name = FieldKeys.FIELD_AUTH, fields = Seq(
            KeywordFieldDefinition(name = FieldKeys.FIELD_TYPE),
            ObjectFieldDefinition(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
          )),
          KeywordFieldDefinition(name = FieldKeys.FIELD_METHOD),
          NestedFieldDefinition(name = FieldKeys.FIELD_PATH, fields = Seq(
            KeywordFieldDefinition(name = FieldKeys.FIELD_KEY),
            TextFieldDefinition(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicFieldDefinition(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          NestedFieldDefinition(name = FieldKeys.FIELD_QUERY, fields = Seq(
            KeywordFieldDefinition(name = FieldKeys.FIELD_KEY),
            TextFieldDefinition(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicFieldDefinition(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          NestedFieldDefinition(name = FieldKeys.FIELD_HEADER, fields = Seq(
            KeywordFieldDefinition(name = FieldKeys.FIELD_KEY),
            TextFieldDefinition(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicFieldDefinition(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          NestedFieldDefinition(name = FieldKeys.FIELD_COOKIE, fields = Seq(
            KeywordFieldDefinition(name = FieldKeys.FIELD_KEY),
            TextFieldDefinition(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicFieldDefinition(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          KeywordFieldDefinition(name = FieldKeys.FIELD_CONTENT_TYPE),
          NestedFieldDefinition(name = FieldKeys.FIELD_BODY, fields = Seq(
            KeywordFieldDefinition(name = FieldKeys.FIELD_CONTENT_TYPE),
            TextFieldDefinition(name = FieldKeys.FIELD_DATA, analysis = EsConfig.IK_ANALYZER),
          )),
        )),
      ObjectFieldDefinition(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
    )
  )
}
