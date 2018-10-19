package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.core.es.model._
import asura.core.exceptions.OperateDocFailException
import com.sksamuel.elastic4s.http.Response
import com.sksamuel.elastic4s.http.bulk.BulkResponse
import com.sksamuel.elastic4s.http.delete.DeleteResponse
import com.sksamuel.elastic4s.http.index.IndexResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.update.UpdateResponse

trait CommonService {

  val aggsTermName = "aggs"
  val defaultIncludeFields = Seq(FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION)
  val defaultExcludeFields = Seq(FieldKeys.FIELD_CREATOR, FieldKeys.FIELD_CREATED_AT)

  def toIndexDocResponse(response: Response[IndexResponse]): IndexDocResponse = {
    if (response.isSuccess) {
      IndexDocResponse(response.result.id)
    } else {
      throw new OperateDocFailException(response.error.reason)
    }
  }

  def toBulkDocResponse(response: Response[BulkResponse]): BulkDocResponse = {
    if (response.isSuccess) {
      BulkDocResponse()
    } else {
      throw new OperateDocFailException(response.error.reason)
    }
  }

  def toDeleteDocResponse(response: Response[DeleteResponse]): DeleteDocResponse = {
    if (response.isSuccess) {
      DeleteDocResponse()
    } else {
      throw new OperateDocFailException(response.error.reason)
    }
  }

  def toDeleteDocResponseFromBulk(response: Response[BulkResponse]): DeleteDocResponse = {
    if (response.isSuccess) {
      DeleteDocResponse()
    } else {
      throw new OperateDocFailException(response.error.reason)
    }
  }

  def toSingleClass[T](response: Response[SearchResponse], id: String)(block: String => T): T = {
    if (response.isSuccess) {
      if (response.result.isEmpty) {
        block(null)
      } else {
        val hit = response.result.hits.hits(0)
        block(hit.sourceAsString)
      }
    } else {
      throw RequestFailException(response.error.reason)
    }
  }

  def toUpdateDocResponse(response: Response[UpdateResponse]): UpdateDocResponse = {
    if (response.isSuccess) {
      UpdateDocResponse(id = response.result.id, result = response.result.result)
    } else {
      throw new OperateDocFailException(response.error.reason)
    }
  }
}
