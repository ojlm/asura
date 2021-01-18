package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.es.model.Label.LabelRef
import asura.core.util.JacksonSupport
import com.sksamuel.elastic4s.mappings._

import scala.collection.mutable

case class FileNode(
                     summary: String = null,
                     description: String = null,
                     group: String,
                     project: String,
                     `type`: String, // 'file' or 'folder'
                     var parent: String = null,
                     var path: Seq[DocRef] = Nil,
                     var size: Long = 0L,
                     extension: String = null,
                     app: String = null,
                     labels: Seq[LabelRef] = Nil,
                     var data: Map[String, Any] = null, // should be small
                     var creator: String = null,
                     var createdAt: String = null,
                     var updatedAt: String = null,
                   ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (null != path) {
      m += (FieldKeys.FIELD_PATH -> JacksonSupport.mapper.convertValue(path, classOf[java.util.List[Map[String, Any]]]))
    }
    if (null != labels) {
      m += (FieldKeys.FIELD_LABELS -> JacksonSupport.mapper.convertValue(labels, classOf[java.util.List[Map[String, Any]]]))
    }
    if (null != data) {
      m += (FieldKeys.FIELD_DATA -> JacksonSupport.mapper.convertValue(data, classOf[java.util.List[Map[String, Any]]]))
    }
    m += (FieldKeys.FIELD_SIZE -> size)
    m += (FieldKeys.FIELD_PARENT -> parent)
    m += (FieldKeys.FIELD_EXTENSION -> `extension`)
    m += (FieldKeys.FIELD_APP -> app)
    m.toMap
  }

}

object FileNode extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}file-node"
  val mappings: MappingDefinition = Es6MappingDefinition(
    BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      NestedField(name = FieldKeys.FIELD_PATH, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_ID),
      )),
      KeywordField(name = FieldKeys.FIELD_PARENT),
      BasicField(name = FieldKeys.FIELD_SIZE, `type` = "long"),
      KeywordField(name = FieldKeys.FIELD_EXTENSION),
      KeywordField(name = FieldKeys.FIELD_APP),
      NestedField(name = FieldKeys.FIELD_LABELS, fields = Seq(
        KeywordField(name = FieldKeys.FIELD_NAME),
      )),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Option("false")),
    )
  )

  val TYPE_FILE = "file"
  val TYPE_FOLDER = "folder"

  val APP_KARATE = "karate"
  val APP_SOLOPI = "solopi"
  val APP_WEB_MONKEY = "web.monkey"

  def isNameLegal(name: String): Boolean = StringUtils.isNotEmpty(name)

}
