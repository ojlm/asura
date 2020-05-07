package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsResponse}
import asura.core.model.QueryDubboRequest
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Iterable, mutable}
import scala.concurrent.Future

object DubboRequestService extends CommonService with BaseAggregationService {

  val basicFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_LABELS,
    FieldKeys.FIELD_OBJECT_REQUEST_INTERFACE,
    FieldKeys.FIELD_OBJECT_REQUEST_METHOD,
    FieldKeys.FIELD_OBJECT_REQUEST_PARAMETER_TYPES,
  )
  val queryFields = basicFields ++ Seq(
    FieldKeys.FIELD_EXPORTS,
  )

  def index(doc: DubboRequest): Future[IndexDocResponse] = {
    val error = validate(doc)
    if (null == error) {
      EsClient.esClient.execute {
        indexInto(DubboRequest.Index).doc(doc).refresh(RefreshPolicy.WAIT_FOR)
      }.map(toIndexDocResponse(_))
    } else {
      error.toFutureFail
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(DubboRequest.Index).refresh(RefreshPolicy.WAIT_FOR)
      }.map(toDeleteDocResponse(_))
    }
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(ids.map(id => delete(id).from(DubboRequest.Index)))
      }.map(toBulkDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(DubboRequest.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def getRequestById(id: String): Future[DubboRequest] = {
    EsClient.esClient.execute {
      search(DubboRequest.Index).query(idsQuery(id)).size(1)
    }.map(res => {
      if (res.isSuccess && res.result.nonEmpty) {
        JacksonSupport.parse(res.result.hits.hits(0).sourceAsString, classOf[DubboRequest])
      } else {
        null
      }
    })
  }

  private def getByIds(ids: Seq[String], filterFields: Boolean = false) = {
    if (null != ids) {
      EsClient.esClient.execute {
        search(DubboRequest.Index)
          .query(idsQuery(ids))
          .from(0)
          .size(ids.length)
          .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
          .sourceInclude(if (filterFields) queryFields else Nil)
      }
    } else {
      ErrorMessages.error_EmptyId.toFutureFail
    }
  }

  def getByIdsAsMap(ids: Seq[String], filterFields: Boolean = false): Future[Map[String, DubboRequest]] = {
    if (null != ids && ids.nonEmpty) {
      val map = mutable.HashMap[String, DubboRequest]()
      getByIds(ids, filterFields).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdsNotFound(ids).toException
          } else {
            res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[DubboRequest])))
            map.toMap
          }
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  def getByIdsAsRawMap(ids: Iterable[String]) = {
    if (null != ids && ids.nonEmpty) {
      EsClient.esClient.execute {
        search(DubboRequest.Index).query(idsQuery(ids)).size(ids.size).sourceInclude(basicFields)
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  def query(q: QueryDubboRequest): Future[Map[String, Any]] = {
    val esQueries = ArrayBuffer[Query]()
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    if (StringUtils.isNotEmpty(q.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, q.group)
    if (StringUtils.isNotEmpty(q.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, q.project)
    if (StringUtils.isNotEmpty(q.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, q.text)
      sortFields = Nil
    }
    if (StringUtils.isNotEmpty(q.interface)) esQueries += wildcardQuery(FieldKeys.FIELD_OBJECT_REQUEST_INTERFACE, s"*${q.interface}*")
    if (StringUtils.isNotEmpty(q.method)) esQueries += termQuery(FieldKeys.FIELD_OBJECT_REQUEST_METHOD, q.method)
    EsClient.esClient.execute {
      search(DubboRequest.Index).query(boolQuery().must(esQueries))
        .from(q.pageFrom)
        .sortBy(sortFields)
        .sourceInclude(queryFields)
    }.flatMap(res => {
      fetchWithCreatorProfiles(res)
      if (res.isSuccess) {
        if (q.hasCreators) {
          fetchWithCreatorProfiles(res)
        } else {
          Future.successful(EsResponse.toApiData(res.result, true))
        }
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    })
  }

  def updateDoc(id: String, doc: DubboRequest) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = validate(doc)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          val (src, params) = doc.toUpdateScriptParams
          update(id).in(DubboRequest.Index)
            .script {
              script(src).params(params)
            }
        }.map(toUpdateDocResponse(_))
      }
    }
  }

  def validate(doc: DubboRequest): ErrorMessage = {
    if (null == doc || null == doc.request) {
      ErrorMessages.error_EmptyRequestBody
    } else if (StringUtils.isEmpty(doc.group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(doc.project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.hasEmpty(doc.request.interface, doc.request.method, doc.request.address)
      || null == doc.request.parameterTypes || null == doc.request.args) {
      ErrorMessages.error_InvalidRequestParameters
    } else {
      null
    }
  }
}
