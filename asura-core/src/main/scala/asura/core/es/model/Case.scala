package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings.{BasicField, _}

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
                 @deprecated("use env")
                 val namespace: String = null,
                 @deprecated("use env")
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
    (sb.toString, m.toMap)
  }
}

object Case extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}case"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_ENV),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
      ObjectField(
        name = FieldKeys.FIELD_REQUEST,
        fields = Seq(
          KeywordField(name = FieldKeys.FIELD_PROTOCOL),
          KeywordField(name = FieldKeys.FIELD_HOST),
          KeywordField(name = FieldKeys.FIELD_RAW_URL, copyTo = Seq(FieldKeys.FIELD__TEXT), index = Option("false")),
          KeywordField(name = FieldKeys.FIELD_URL_PATH),
          BasicField(name = FieldKeys.FIELD_PORT, `type` = "integer"),
          ObjectField(name = FieldKeys.FIELD_AUTH, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_TYPE),
            ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
          )),
          KeywordField(name = FieldKeys.FIELD_METHOD),
          NestedField(name = FieldKeys.FIELD_PATH, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_KEY),
            TextField(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          NestedField(name = FieldKeys.FIELD_QUERY, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_KEY),
            TextField(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          NestedField(name = FieldKeys.FIELD_HEADER, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_KEY),
            TextField(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          NestedField(name = FieldKeys.FIELD_COOKIE, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_KEY),
            TextField(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
          )),
          KeywordField(name = FieldKeys.FIELD_CONTENT_TYPE),
          NestedField(name = FieldKeys.FIELD_BODY, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_CONTENT_TYPE),
            TextField(name = FieldKeys.FIELD_DATA, analysis = EsConfig.IK_ANALYZER),
          )),
        )),
      ObjectField(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
    )
  )
}
