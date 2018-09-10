package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class Scenario(
                     val summary: String,
                     val description: String,
                     val group: String,
                     val project: String,
                     val cases: Seq[DocRef],
                     val labels: Seq[LabelRef] = Nil,
                     var creator: String = null,
                     var createdAt: String = null,
                   ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != cases) {
      m += (FieldKeys.FIELD_CASES -> cases)
    }
    if (null != labels) {
      m += (FieldKeys.FIELD_LABELS -> labels)
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
      NestedField(name = FieldKeys.FIELD_CASES, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_ID),
      )),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
    )
  )
}
