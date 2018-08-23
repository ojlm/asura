package asura.core.es.model

import asura.common.model.ApiType
import asura.common.util.StringUtils
import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

/** id:group_project_method_path not _id */
case class RestApi(
                    val summary: String = StringUtils.EMPTY,
                    val description: String = StringUtils.EMPTY,
                    val path: String,
                    val method: String,
                    val group: String,
                    val project: String,
                    val service: String = null,
                    var id: String = null,
                    var deprecated: Boolean = false,
                    var version: String = StringUtils.EMPTY,
                    var `type`: String = ApiType.REST,
                    var labels: Seq[LabelRef] = Nil,
                    var schema: RestApiSchema = null,
                    var creator: String = null,
                    var createdAt: String = null,
                  ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {

    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (StringUtils.isNotEmpty(path)) {
      m += (FieldKeys.FIELD_PATH -> path)
    }
    if (StringUtils.isNotEmpty(method)) {
      m += (FieldKeys.FIELD_METHOD -> method)
    }
    if (StringUtils.isNotEmpty(version)) {
      m += (FieldKeys.FIELD_VERSION -> version)
    }
    if (deprecated eq null) {
      m += (FieldKeys.FIELD_DEPRECATED -> deprecated)
    }
    if (StringUtils.isNotEmpty(`type`)) {
      m += (FieldKeys.FIELD_ID -> `type`)
    }
    if (null != labels && labels.nonEmpty) {
      m += (FieldKeys.FIELD_LABELS -> labels)
    }
    if (null != schema) {
      m += (FieldKeys.FIELD_SCHEMA -> schema)
    }
    if (null != service) {
      m += (FieldKeys.FIELD_SERVICE -> service)
    }
    m.toMap
  }

  override def equals(obj: scala.Any): Boolean = {
    if (null != obj && obj.isInstanceOf[RestApi]) {
      val target = obj.asInstanceOf[RestApi]
      path.equals(target.path) && method.equals(target.method) && version.equals(version)
    } else {
      false
    }
  }

  /** id for check if the api exists,
    * not use _id because the if _id can be used in url and path has ambiguity character
    */
  def generateId(): String = s"${group}_${project}_${method}_${path}"

}

object RestApi extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}api"
  val mappings: MappingDefinition = MappingDefinition(
    `type` = EsConfig.DefaultType,
    fields = BaseIndex.fieldDefinitions ++ Seq(
      KeywordFieldDefinition(name = FieldKeys.FIELD_ID),
      KeywordFieldDefinition(name = FieldKeys.FIELD_PATH, copyTo = Seq(FieldKeys.FIELD__TEXT)),
      KeywordFieldDefinition(name = FieldKeys.FIELD_METHOD),
      KeywordFieldDefinition(name = FieldKeys.FIELD_GROUP),
      KeywordFieldDefinition(name = FieldKeys.FIELD_PROJECT),
      KeywordFieldDefinition(name = FieldKeys.FIELD_SERVICE),
      BasicFieldDefinition(name = FieldKeys.FIELD_DEPRECATED, `type` = "boolean"),
      KeywordFieldDefinition(name = FieldKeys.FIELD_VERSION),
      KeywordFieldDefinition(name = FieldKeys.FIELD_TYPE),
      NestedFieldDefinition(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordFieldDefinition(name = FieldKeys.FIELD_NAME),
      )),
      ObjectFieldDefinition(name = FieldKeys.FIELD_SCHEMA, dynamic = Some("false")),
    )
  )
}
