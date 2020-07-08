package asura.core.es.model

case class IndexDocResponse(id: String)

case class DeleteDocResponse()

case class UpdateDocResponse(id: String, result: String)

case class BulkDocResponse(count: Long)

case class DeleteByQueryRes(
                             total: Long,
                             deleted: Long,
                             batches: Long,
                           )
