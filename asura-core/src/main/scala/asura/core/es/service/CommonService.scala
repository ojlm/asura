package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.util.StringUtils
import asura.core.es.model._
import asura.core.exceptions.OperateDocFailException
import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.requests.bulk.BulkResponse
import com.sksamuel.elastic4s.requests.delete.{DeleteByQueryResponse, DeleteResponse}
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.indexes.admin.DeleteIndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticRequest, Handler, Response}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait CommonService {

  val defaultIncludeFields = Seq(
    FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_GROUP, FieldKeys.FIELD_PROJECT
  )
  val defaultExcludeFields = Seq(FieldKeys.FIELD_CREATOR, FieldKeys.FIELD_CREATED_AT, FieldKeys.FIELD_UPDATED_AT)

  def toIndexDocResponse(response: Response[IndexResponse]): IndexDocResponse = {
    if (response.isSuccess) {
      IndexDocResponse(response.result.id)
    } else {
      throw OperateDocFailException(response.error.reason)
    }
  }

  def toBulkDocResponse(response: Response[BulkResponse]): BulkDocResponse = {
    if (response.isSuccess) {
      BulkDocResponse(response.result.items.length)
    } else {
      throw OperateDocFailException(response.error.reason)
    }
  }

  def toDeleteDocResponse(response: Response[DeleteResponse]): DeleteDocResponse = {
    if (response.isSuccess) {
      DeleteDocResponse()
    } else {
      throw OperateDocFailException(response.error.reason)
    }
  }

  def toDeleteDocResponseFromBulk(response: Response[BulkResponse]): DeleteDocResponse = {
    if (response.isSuccess) {
      DeleteDocResponse()
    } else {
      throw OperateDocFailException(response.error.reason)
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
      throw OperateDocFailException(response.error.reason)
    }
  }

  def toDeleteIndexResponse(response: Response[DeleteIndexResponse]): DeleteIndexResponse = {
    if (response.isSuccess) {
      response.result
    } else {
      throw OperateDocFailException(response.error.reason)
    }
  }

  def toDeleteByQueryResponse(response: Response[DeleteByQueryResponse]): DeleteByQueryRes = {
    if (response.isSuccess) {
      val result = response.result
      DeleteByQueryRes(result.total, result.deleted, result.batches)
    } else {
      throw OperateDocFailException(response.error.reason)
    }
  }

  def fetchWithCreatorProfiles(res: Response[SearchResponse])(implicit ec: ExecutionContext): Future[Map[String, Any]] = {
    val hits = res.result.hits
    val userIds = mutable.HashSet[String]()
    val dataMap = Map("total" -> hits.total.value, "list" -> hits.hits.map(hit => {
      val sourceMap = hit.sourceAsMap
      userIds += sourceMap.getOrElse(FieldKeys.FIELD_CREATOR, StringUtils.EMPTY).asInstanceOf[String]
      sourceMap + (FieldKeys.FIELD__ID -> hit.id) + (FieldKeys.FIELD__SORT -> hit.sort.getOrElse(Nil))
    }))
    UserProfileService.getByIdsAsRawMap(userIds).map(users => {
      dataMap + ("creators" -> users)
    })
  }
}

object CommonService {

  case class CustomCatIndices(pattern: String)

  case class CustomCatIndicesResponse(
                                       health: String,
                                       status: String,
                                       index: String,
                                       uuid: String,
                                       pri: String,
                                       rep: String,
                                       @JsonProperty("docs.count") count: String,
                                       @JsonProperty("docs.deleted") deleted: String,
                                       @JsonProperty("store.size") storeSize: String,
                                       @JsonProperty("pri.store.size") priStoreSize: String
                                     )

  implicit object CustomCatIndexesHandler extends Handler[CustomCatIndices, Seq[CustomCatIndicesResponse]] {
    override def build(request: CustomCatIndices): ElasticRequest = {
      ElasticRequest("GET", s"/_cat/indices/${request.pattern}?v&format=json&s=index:desc")
    }
  }

}
