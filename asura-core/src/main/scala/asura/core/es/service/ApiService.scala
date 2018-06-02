package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{FieldKeys, RestApi}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object ApiService {

  def index(api: RestApi) = {
    if (null == api) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val (isOK, errMsg) = validate(api)
      if (!isOK) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.httpClient.execute {
          indexInto(RestApi.Index / EsConfig.DefaultType).doc(api).refresh(RefreshPolicy.WAIT_UNTIL)
        }
      }
    }
  }

  def index(apis: Seq[RestApi]) = {
    if (null == apis || apis.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val (isOk, errMsg) = validate(apis)
      if (!isOk) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.httpClient.execute {
          bulk {
            apis.map(api => indexInto(RestApi.Index / EsConfig.DefaultType).doc(api))
          }.refresh(RefreshPolicy.WAIT_UNTIL)
        }
      }
    }
  }

  def getByApis(apis: Seq[RestApi]) = {
    if (null == apis || apis.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val (isOk, errMsg) = validate(apis)
      if (!isOk) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.httpClient.execute {
          search(RestApi.Index).bool(should(
            apis.map(api => must(
              termQuery(FieldKeys.FIELD_PROJECT, api.project),
              termQuery(FieldKeys.FIELD_PATH, api.path),
              termQuery(FieldKeys.FIELD_METHOD, api.method),
              termQuery(FieldKeys.FIELD_VERSION, api.version)
            ))
          ))
        }
      }
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(RestApi.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def deleteDoc(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        bulk(ids.map(id => delete(id).from(RestApi.Index / EsConfig.DefaultType)))
      }
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(RestApi.Index).query(idsQuery(id))
      }
    }
  }

  def getById(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(RestApi.Index).query(idsQuery(ids))
      }
    }
  }

  def getAll(project: String) = {
    EsClient.httpClient.execute {
      search(RestApi.Index)
        .query(termQuery(FieldKeys.FIELD_PROJECT, project))
        .limit(EsConfig.MaxCount)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def updateApi(apiUpdate: ApiUpdate) = {
    if (null == apiUpdate || null == apiUpdate.id || null == apiUpdate.api) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val api = apiUpdate.api
      val (isOk, errMsg) = validate(api)
      if (!isOk) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.httpClient.execute {
          update(apiUpdate.id).in(RestApi.Index / EsConfig.DefaultType).doc(api.toUpdateMap)
        }
      }
    }
  }

  def docCount(path: String, project: String) = {
    EsClient.httpClient.execute {
      count(RestApi.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_PATH, path),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }
    }
  }

  def docCount(path: String, method: String, version: String, project: String) = {
    EsClient.httpClient.execute {
      count(RestApi.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_PATH, path),
          termQuery(FieldKeys.FIELD_METHOD, method),
          termQuery(FieldKeys.FIELD_VERSION, version),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }
    }
  }

  def validate(api: RestApi): BoolErrorRes = {
    if (StringUtils.isEmpty(api.path)) {
      (false, "Empty path")
    } else if (StringUtils.isEmpty(api.method)) {
      (false, "Empty method")
    } else if (StringUtils.isEmpty(api.project)) {
      (false, "Empty project")
    } else if (StringUtils.isEmpty(api.group)) {
      (false, "Empty group")
    } else {
      if (null == api.version) {
        api.version = StringUtils.EMPTY
      }
      (true, null)
    }
  }

  def validate(apis: Seq[RestApi]): BoolErrorRes = {
    var isOk = true
    val apiSet = mutable.Set[RestApi]()
    var errMsg: String = null
    for (i <- 0 until apis.length if isOk) {
      val api = apis(i)
      if (apiSet.contains(api)) {
        isOk = false
        errMsg = s"duplicate api: ${api.path}:${api.method}:${api.version}"
      } else {
        val (b, m) = validate(api)
        if (b) {
          apiSet += api
        } else {
          isOk = false
          errMsg = m
        }
      }
    }
    (isOk, errMsg)
  }

  def getApiById(id: String)(implicit executor: ExecutionContext): Future[RestApi] = {
    if (StringUtils.isEmpty(id)) {
      Future.successful(null)
    } else {
      getById(id).map(res => {
        res match {
          case Right(success) =>
            if (success.result.isEmpty) {
              throw IllegalRequestException(s"Api: ${id} not found.")
            } else {
              val hit = success.result.hits.hits(0)
              JacksonSupport.parse(hit.sourceAsString, classOf[RestApi])
            }
          case Left(failure) =>
            throw RequestFailException(failure.error.reason)
        }
      })
    }
  }

  case class ApiUpdate(
                        id: String,
                        api: RestApi
                      )

}
