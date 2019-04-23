package asura.core.es.model

import asura.core.es.EsConfig
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class SqlRequest(
                       val summary: String,
                       val description: String,
                       val group: String,
                       val project: String,
                       val host: String,
                       val port: Int,
                       val username: String,
                       var password: String,
                       var encryptedPass: String,
                       val database: String,
                       var table: String,
                       val sql: String,
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
    if (null != host) {
      m += (FieldKeys.FIELD_HOST -> host)
      addScriptUpdateItem(sb, FieldKeys.FIELD_HOST)
    }
    if (port > 0) {
      m += (FieldKeys.FIELD_PORT -> port)
      addScriptUpdateItem(sb, FieldKeys.FIELD_PORT)
    }
    if (null != username) {
      m += (FieldKeys.FIELD_USERNAME -> username)
      addScriptUpdateItem(sb, FieldKeys.FIELD_USERNAME)
    }
    // TODO: handle password and encrypted password
    if (null != database) {
      m += (FieldKeys.FIELD_DATABASE -> database)
      addScriptUpdateItem(sb, FieldKeys.FIELD_DATABASE)
    }
    // TODO: parse table from raw sql
    if (null != table) {
      m += (FieldKeys.FIELD_TABLE -> table)
      addScriptUpdateItem(sb, FieldKeys.FIELD_TABLE)
    }
    if (null != sql) {
      m += (FieldKeys.FIELD_SQL -> sql)
      addScriptUpdateItem(sb, FieldKeys.FIELD_SQL)
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

object SqlRequest extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}sql-request"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_HOST),
      BasicField(name = FieldKeys.FIELD_PORT, `type` = "integer"),
      KeywordField(name = FieldKeys.FIELD_USERNAME),
      KeywordField(name = FieldKeys.FIELD_PASSWORD),
      KeywordField(name = FieldKeys.FIELD_ENCRYPTED_PASS, index = Some("false")),
      KeywordField(name = FieldKeys.FIELD_DATABASE),
      KeywordField(name = FieldKeys.FIELD_TABLE),
      TextField(name = FieldKeys.FIELD_SQL, analysis = EsConfig.IK_ANALYZER),
      ObjectField(name = FieldKeys.FIELD_ASSERT, dynamic = Some("false")),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
    )
  )
}
