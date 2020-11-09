package asura.core.es.model

import asura.common.util.DateUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

case class Favorite(
                     var id: String, // logic id for checking existence
                     val group: String,
                     val project: String,
                     val summary: String,
                     var user: String,
                     var `type`: String,
                     var targetType: String, // target resource type
                     val targetId: String,
                     var checked: Boolean = false, // at once the doc is created it will not be deleted
                     var timestamp: String = DateUtils.nowDateTime,
                     var data: Map[String, Any] = null,
                   ) {

  def generateLogicId() = {
    if (Favorite.TYPE_TOP_TOP.equals(`type`)) {
      // this type is user irrelevant
      s"${group}_${project}_${`type`}_${targetType}_${targetId}"
    } else {
      s"${group}_${project}_${user}_${`type`}_${targetType}_${targetId}"
    }
  }
}

object Favorite extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}favorite"
  override val shards: Int = 5
  override val replicas: Int = 1
  val mappings: MappingDefinition = Es6MappingDefinition(
    Seq(
      TextField(name = FieldKeys.FIELD_SUMMARY, copyTo = Seq(FieldKeys.FIELD__TEXT), analysis = EsConfig.IK_ANALYZER),
      TextField(name = FieldKeys.FIELD__TEXT, analysis = EsConfig.IK_ANALYZER),
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_USER),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_TARGET_TYPE),
      KeywordField(name = FieldKeys.FIELD_TARGET_ID),
      KeywordField(name = FieldKeys.FIELD_ID),
      BasicField(name = FieldKeys.FIELD_CHECKED, `type` = "boolean"),
      BasicField(name = FieldKeys.FIELD_TIMESTAMP, `type` = "date", format = Some(EsConfig.DateFormat)),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
    )
  )

  // show in single component in top route
  val TYPE_TOP_TOP = "toptop"
  // people
  val TYPE_WATCH = "watch"
  // resource
  val TYPE_STAR = "star"
  // people
  val TYPE_FOLLOW = "follow"


  val TARGET_TYPE_SCENARIO = FieldKeys.FIELD_SCENARIO
  val TARGET_TYPE_JOB = FieldKeys.FIELD_JOB
}
