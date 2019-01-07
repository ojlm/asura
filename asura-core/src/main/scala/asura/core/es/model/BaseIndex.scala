package asura.core.es.model

import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{BasicField, FieldDefinition, KeywordField, TextField}

import scala.collection.mutable

trait BaseIndex {

  val summary: String
  val description: String
  var creator: String
  var createdAt: String
  var updatedAt: String

  def checkCommFieldsToUpdate(m: mutable.Map[String, Any], sb: StringBuilder = null): Unit = {
    if (null != summary) {
      m += (FieldKeys.FIELD_SUMMARY -> summary)
      if (null != sb) addScriptUpdateItem(sb, FieldKeys.FIELD_SUMMARY)
    }
    if (null != description) {
      m += (FieldKeys.FIELD_DESCRIPTION -> description)
      if (null != sb) addScriptUpdateItem(sb, FieldKeys.FIELD_DESCRIPTION)
    }
    m += (FieldKeys.FIELD_UPDATED_AT -> DateUtils.nowDateTime)
    if (null != sb) addScriptUpdateItem(sb, FieldKeys.FIELD_UPDATED_AT)
  }

  def fillCommonFields(creator: String): Unit = {
    this.creator = if (StringUtils.isNotEmpty(creator)) creator else StringUtils.EMPTY
    this.createdAt = DateUtils.nowDateTime
    this.updatedAt = this.createdAt
  }

  def toUpdateMap: Map[String, Any] = Map.empty

  // if there is dynamic object filed, use this method to update the whole filed by painless script.
  def toUpdateScriptParams: (String, Map[String, Any]) = (StringUtils.EMPTY, Map.empty)

  def addScriptUpdateItem(sb: mutable.StringBuilder, filedKey: String): Unit = {
    sb.append("ctx._source.").append(filedKey).append(" = params.").append(filedKey).append(";")
  }
}

object BaseIndex {

  val fieldDefinitions: Seq[FieldDefinition] = Seq(
    TextField(name = FieldKeys.FIELD_SUMMARY, copyTo = Seq(FieldKeys.FIELD__TEXT), analysis = EsConfig.IK_ANALYZER),
    TextField(name = FieldKeys.FIELD_DESCRIPTION, copyTo = Seq(FieldKeys.FIELD__TEXT), analysis = EsConfig.IK_ANALYZER),
    TextField(name = FieldKeys.FIELD__TEXT, analysis = EsConfig.IK_ANALYZER),
    KeywordField(name = FieldKeys.FIELD_CREATOR),
    BasicField(name = FieldKeys.FIELD_CREATED_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
    BasicField(name = FieldKeys.FIELD_UPDATED_AT, `type` = "date", format = Some(EsConfig.DateFormat)),
  )

  /** user login by ldap */
  val CREATOR_LDAP = "ldap"
  /** user login not by ldap */
  val CREATOR_STANDARD = "standard"
  /** used for quartz job */
  val CREATOR_QUARTZ = "quartz"
  /** used for ci call */
  val CREATOR_CI = "ci"
}
