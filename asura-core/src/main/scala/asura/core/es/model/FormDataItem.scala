package asura.core.es.model

import asura.core.es.model.FormDataItem.BlobMetaData

case class FormDataItem(
                         key: String,
                         value: String,
                         `type`: String, // 'string' or 'blob'
                         enabled: Boolean = true,
                         description: String = null,
                         metaData: BlobMetaData = null,
                       )

object FormDataItem {

  val TYPE_STRING = "string"
  val TYPE_BLOB = "blob"

  case class BlobMetaData(
                           engine: String,
                           key: String,
                           fileName: String,
                           contentLength: Long,
                           contentType: String,
                         )

}
