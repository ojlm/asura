package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.es.model.Label.LabelRef
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings.{BasicField, KeywordField, MappingDefinition, NestedField}

import scala.collection.mutable

case class DomainOnlineConfig(
                               val summary: String,
                               val description: String,
                               val domain: String,
                               val tag: String = StringUtils.EMPTY,
                               val maxApiCount: Int = 2000,
                               val minReqCount: Int = 0,
                               val exMethods: Seq[LabelRef] = Nil,
                               val exSuffixes: String = StringUtils.EMPTY,
                               val inclusions: Seq[FieldPattern] = Nil, // online api include pattern
                               val exclusions: Seq[FieldPattern] = Nil, // online api exclude pattern
                               var creator: String = null,
                               var createdAt: String = null,
                               var updatedAt: String = null,
                             ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (StringUtils.isNotEmpty(domain)) {
      m += (FieldKeys.FIELD_DOMAIN -> domain)
    }
    if (null != tag) {
      m += (FieldKeys.FIELD_TAG -> tag)
    }
    if (Option(maxApiCount).nonEmpty && maxApiCount > 0) {
      m += (FieldKeys.FIELD_MAX_API_COUNT -> maxApiCount)
    }
    if (Option(minReqCount).nonEmpty && minReqCount > 0) {
      m += (FieldKeys.FIELD_MIN_REQ_COUNT -> minReqCount)
    }
    if (null != exMethods) {
      m += (FieldKeys.FIELD_EX_METHODS -> JacksonSupport.mapper.convertValue(inclusions, classOf[java.util.List[Map[String, Any]]]))
    }
    if (null != inclusions) {
      m += (FieldKeys.FIELD_INCLUSIONS -> JacksonSupport.mapper.convertValue(inclusions, classOf[java.util.List[Map[String, Any]]]))
    }
    if (null != exclusions) {
      m += (FieldKeys.FIELD_EXCLUSIONS -> JacksonSupport.mapper.convertValue(exclusions, classOf[java.util.List[Map[String, Any]]]))
    }
    if (null != exSuffixes) {
      m += (FieldKeys.FIELD_EX_SUFFIXES -> exSuffixes)
    }
    m.toMap
  }

  def generateDocId() = s"${domain}${if (StringUtils.isNotEmpty(tag)) s"_${tag}" else StringUtils.EMPTY}"
}

object DomainOnlineConfig extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}domain-online-config"
  override val shards: Int = 1
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_DOMAIN),
      KeywordField(name = FieldKeys.FIELD_TAG),
      BasicField(name = FieldKeys.FIELD_MAX_API_COUNT, `type` = "integer"),
      BasicField(name = FieldKeys.FIELD_MIN_REQ_COUNT, `type` = "integer"),
      KeywordField(name = FieldKeys.FIELD_EX_SUFFIXES, index = Option("false")),
      NestedField(name = FieldKeys.FIELD_EX_METHODS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
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
