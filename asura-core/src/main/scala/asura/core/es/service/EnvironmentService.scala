package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryEnv
import asura.core.es.model.{DeleteDocResponse, Environment, FieldKeys, IndexDocResponse}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

object EnvironmentService extends CommonService {

  val logger = Logger("EnvironmentService")

  def index(env: Environment): Future[IndexDocResponse] = {
    val error = validate(env)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.esClient.execute {
        indexInto(Environment.Index / EsConfig.DefaultType).doc(env).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        delete(id).from(Environment.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(Environment.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def getAll(project: String) = {
    EsClient.esClient.execute {
      search(Environment.Index)
        .query(termQuery(FieldKeys.FIELD_PROJECT, project))
        .limit(EsConfig.MaxCount)
        .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def updateEnv(id: String, env: Environment) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = validate(env)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          update(id).in(Environment.Index / EsConfig.DefaultType)
            .doc(JacksonSupport.stringify(env.toUpdateMap))
            .refresh(RefreshPolicy.WAIT_UNTIL)
        }
      }
    }
  }

  def validate(env: Environment): ErrorMessages.Val = {
    if (StringUtils.isEmpty(env.summary)) {
      ErrorMessages.error_EmptySummary
    } else if (StringUtils.isEmpty(env.group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(env.project)) {
      ErrorMessages.error_EmptyProject
    } else {
      null
    }
  }

  def getEnvById(id: String)(implicit executor: ExecutionContext): Future[Environment] = {
    if (StringUtils.isEmpty(id)) {
      Future.successful(null)
    } else {
      getById(id).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw IllegalRequestException(s"Env: ${id} not found.")
          } else {
            val hit = res.result.hits.hits(0)
            JacksonSupport.parse(hit.sourceAsString, classOf[Environment])
          }
        } else {
          throw RequestFailException(res.error.reason)
        }
      })
    }
  }

  def queryEnv(query: QueryEnv) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    EsClient.esClient.execute {
      search(Environment.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields :+ FieldKeys.FIELD_PATH :+ FieldKeys.FIELD_METHOD)
    }
  }
}
