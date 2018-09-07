package asura.core.es.model

case class IndexDocResponse(id: String)

case class DeleteDocResponse()

case class UpdateDocResponse(id: String, result: String)

case class BulkDocResponse()
