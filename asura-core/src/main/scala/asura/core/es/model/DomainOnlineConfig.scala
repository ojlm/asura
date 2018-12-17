package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition, NestedField}

import scala.collection.mutable

case class DomainOnlineConfig(
                               val summary: String,
                               val description: String,
                               val domain: String,
                               val maxApiCount: Int,
                               val inclusions: Seq[FieldPattern] = Nil, // online api include pattern
                               val exclusions: Seq[FieldPattern] = Nil, // online api exclude pattern
                               var creator: String = null,
                               var createdAt: String = null,
                             ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (StringUtils.isNotEmpty(domain)) {
      m += (FieldKeys.FIELD_DOMAIN -> domain)
    }
    if (Option(maxApiCount).nonEmpty && maxApiCount > 0) {
      m += (FieldKeys.FIELD_MAX_API_COUNT -> maxApiCount)
    }
    if (null != inclusions) {
      m += (FieldKeys.FIELD_INCLUSIONS -> JacksonSupport.mapper.convertValue(inclusions, classOf[java.util.List[Map[String, Any]]]))
    }
    if (null != exclusions) {
      m += (FieldKeys.FIELD_EXCLUSIONS -> JacksonSupport.mapper.convertValue(exclusions, classOf[java.util.List[Map[String, Any]]]))
    }
    m.toMap
  }
}

object DomainOnlineConfig extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}domain-online-config"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_DOMAIN),
      BasicField(name = FieldKeys.FIELD_MAX_API_COUNT, `type` = "integer"),
      NestedField(name = FieldKeys.FIELD_INCLUSIONS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_FIELD),
        KeywordField(name = FieldKeys.FIELD_VALUE),
        KeywordField(name = FieldKeys.FIELD_ALIAS),
        KeywordField(name = FieldKeys.FIELD_TYPE),
      )),
      NestedField(name = FieldKeys.FIELD_EXCLUSIONS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_FIELD),
        KeywordField(name = FieldKeys.FIELD_VALUE),
        KeywordField(name = FieldKeys.FIELD_ALIAS),
        KeywordField(name = FieldKeys.FIELD_TYPE),
      )),
    )
  )
}
