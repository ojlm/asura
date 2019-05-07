package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class Scenario(
                     val summary: String,
                     val description: String,
                     val group: String,
                     val project: String,
                     val steps: Seq[ScenarioStep],
                     val env: String = StringUtils.EMPTY,
                     val labels: Seq[LabelRef] = Nil,
                     var creator: String = null,
                     var createdAt: String = null,
                     var updatedAt: String = null,
                   ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != steps) {
      m += (FieldKeys.FIELD_STEPS -> steps)
    }
    if (null != labels) {
      m += (FieldKeys.FIELD_LABELS -> labels)
    }
    if (null != env) {
      m += (FieldKeys.FIELD_ENV -> env)
    }
    m.toMap
  }
}

object Scenario extends IndexSetting {
  val Index: String = s"${EsConfig.IndexPrefix}scenario"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      NestedField(name = FieldKeys.FIELD_STEPS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_ID),
        KeywordField(name = FieldKeys.FIELD_TYPE),
        BasicField(name = FieldKeys.FIELD_STORED, `type` = "boolean"),
        BasicField(name = FieldKeys.FIELD_ENABLED, `type` = "boolean"),
      )),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
        ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
      )),
      KeywordField(name = FieldKeys.FIELD_ENV),
    )
  )
}
