package asura.core.es.model

case class IndexDocResponse(id: String)

case class BulkIndexDocResponse()

case class DeleteDocResponse()

case class UpdateDocResponse(id: String, result: String)
