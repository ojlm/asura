package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.es.model.Label.LabelRef
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings.{BasicField, _}

import scala.collection.mutable

/**
 * If `useProxy` is `true`, then use `namespace` in env first then can fall back to `namespace`
 */
case class HttpStepRequest(
                            summary: String,
                            description: String,
                            var group: String,
                            var project: String,
                            request: Request,
                            assert: Map[String, Any],
                            env: String = StringUtils.EMPTY,
                            labels: Seq[LabelRef] = Nil,
                            var copyFrom: String = null,
                            generator: RequestGenerator = RequestGenerator(),
                            exports: Seq[VariablesExportItem] = Nil,
                            var creator: String = null,
                            var createdAt: String = null,
                            var updatedAt: String = null,
                          ) extends BaseIndex {

  override def toUpdateScriptParams: (String, Map[String, Any]) = {
    val sb = new StringBuilder()
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
    if (null != labels) {
      m += (FieldKeys.FIELD_LABELS -> JacksonSupport.mapper.convertValue(labels, classOf[java.util.List[Map[String, Any]]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_LABELS)
    }
    if (null != copyFrom) {
      m += (FieldKeys.FIELD_COPY_FROM -> copyFrom)
      addScriptUpdateItem(sb, FieldKeys.FIELD_COPY_FROM)
    }
    if (null != assert) {
      m += (FieldKeys.FIELD_ASSERT -> assert)
      addScriptUpdateItem(sb, FieldKeys.FIELD_ASSERT)
    }
    if (null != generator) {
      m += (FieldKeys.FIELD_GENERATOR -> JacksonSupport.mapper.convertValue(generator, classOf[java.util.Map[String, Any]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_GENERATOR)
    }
    if (null != exports) {
      m += (FieldKeys.FIELD_EXPORTS -> JacksonSupport.mapper.convertValue(exports, classOf[java.util.List[Map[String, Any]]]))
      addScriptUpdateItem(sb, FieldKeys.FIELD_EXPORTS)
    }
    (sb.toString, m.toMap)
  }

  def calcGeneratorCount(): Unit = {
    if (null != generator) {
      generator.count = if (StringUtils.isNotEmpty(generator.script)) generator.list.size + 1 else generator.list.size
    }
  }
}

object HttpStepRequest extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}case"
  val mappings: MappingDefinition = Es6MappingDefinition(
    BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_ENV),
      KeywordField(name = FieldKeys.FIELD_COPY_FROM),
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
            TextField(name = FieldKeys.FIELD_DESCRIPTION, index = Some("false")),
          )),
          NestedField(name = FieldKeys.FIELD_QUERY, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_KEY),
            TextField(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
            TextField(name = FieldKeys.FIELD_DESCRIPTION, index = Some("false")),
          )),
          NestedField(name = FieldKeys.FIELD_HEADER, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_KEY),
            TextField(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
            TextField(name = FieldKeys.FIELD_DESCRIPTION, index = Some("false")),
          )),
          NestedField(name = FieldKeys.FIELD_COOKIE, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_KEY),
            TextField(name = FieldKeys.FIELD_VALUE, analysis = EsConfig.IK_ANALYZER),
            BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
            TextField(name = FieldKeys.FIELD_DESCRIPTION, index = Some("false")),
          )),
          KeywordField(name = FieldKeys.FIELD_CONTENT_TYPE),
          NestedField(name = FieldKeys.FIELD_BODY, fields = Seq(
            KeywordField(name = FieldKeys.FIELD_CONTENT_TYPE),
            TextField(name = FieldKeys.FIELD_DATA, analysis = EsConfig.IK_ANALYZER),
          )),
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
}
