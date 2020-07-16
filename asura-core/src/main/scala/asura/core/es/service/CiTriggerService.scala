package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, JsonUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model._
import asura.core.model.QueryTrigger
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import asura.core.{ErrorMessages => CoreErrorMessages}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.typesafe.scalalogging.Logger

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object CiTriggerService extends CommonService {

  val logger = Logger("CiTriggerService")

  def index(doc: CiTrigger): Future[IndexDocResponse] = {
    val errorMsg = validate(doc)
    if (null == errorMsg) {
      EsClient.esClient.execute {
        indexInto(CiTrigger.Index).doc(doc).refresh(RefreshPolicy.WAIT_FOR)
      }.map(toIndexDocResponse(_))
    } else {
      errorMsg.toFutureFail
    }
  }

  def deleteTrigger(id: String): Future[DeleteDocResponse] = {
    EsClient.esClient.execute {
      delete(id).from(CiTrigger.Index).refresh(RefreshPolicy.WAIT_FOR)
    }.map(toDeleteDocResponse)
  }

  def getTriggerById(id: String): Future[CiTrigger] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(CiTrigger.Index).query(idsQuery(id)).size(1)
      }.map(res => {
        if (res.isSuccess && res.result.nonEmpty) {
          JacksonSupport.parse(res.result.hits.hits(0).sourceAsString, classOf[CiTrigger])
        } else {
          throw CoreErrorMessages.error_EsRequestFail(res).toException
        }
      })
    }
  }

  def queryTrigger(query: QueryTrigger) = {
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    if (StringUtils.isNotEmpty(query.text)) sortFields = Nil
    EsClient.esClient.execute {
      search(CiTrigger.Index).query(boolQuery().must(buildEsQueries(query)))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(sortFields)
    }
  }

  def getTriggersAsMap(query: QueryTrigger): Future[Map[String, CiTrigger]] = {
    val map = mutable.HashMap[String, CiTrigger]()
    EsClient.esClient.execute {
      search(CiTrigger.Index).query(boolQuery().must(buildEsQueries(query)))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }.map(res => {
      if (res.isSuccess) {
        if (res.result.nonEmpty) {
          res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[CiTrigger])))
        }
        map.toMap
      } else {
        throw CoreErrorMessages.error_EsRequestFail(res).toException
      }
    })
  }

  private def buildEsQueries(query: QueryTrigger): collection.Seq[Query] = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    }
    query.enabled match {
      case "true" => esQueries += termQuery(FieldKeys.FIELD_ENABLED, true)
      case "false" => esQueries += termQuery(FieldKeys.FIELD_ENABLED, false)
      case _ =>
    }
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.env)) esQueries += termQuery(FieldKeys.FIELD_ENV, query.env)
    if (StringUtils.isNotEmpty(query.service)) esQueries += termQuery(FieldKeys.FIELD_SERVICE, query.service)
    esQueries
  }

  def updateDoc(id: String, doc: CiTrigger) = {
    if (StringUtils.isEmpty(id)) {
      CoreErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = validate(doc)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          update(id).in(CiTrigger.Index).doc(JsonUtils.stringify(doc.toUpdateMap))
        }.map(toUpdateDocResponse(_))
      }
    }
  }

  def validate(doc: CiTrigger): ErrorMessage = {
    if (StringUtils.isEmpty(doc.group)) {
      CoreErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(doc.project)) {
      CoreErrorMessages.error_EmptyProject
    } else if (StringUtils.hasEmpty(doc.targetType, doc.targetId)) {
      CoreErrorMessages.error_InvalidParams
    } else {
      if (null != doc.readiness && doc.readiness.enabled
        && StringUtils.hasEmpty(doc.readiness.targetId, doc.readiness.targetType)) {
        CoreErrorMessages.error_InvalidParams
      } else {
        null
      }
    }
  }
}
