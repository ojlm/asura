package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{BulkDocResponse, FieldKeys, IndexDocResponse, RestApi}
import asura.core.es.{EsClient, EsConfig}
import asura.core.model.QueryApi
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object ApiService extends CommonService {

  def index(api: RestApi): Future[IndexDocResponse] = {
    if (null == api) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val error = validate(api)
      if (null != error) {
        error.toFutureFail
      } else {
        api.id = api.generateId()
        EsClient.esClient.execute {
          indexInto(RestApi.Index / EsConfig.DefaultType)
            .doc(api)
            .refresh(RefreshPolicy.WAIT_UNTIL)
        }.map(toIndexDocResponse(_))
      }
    }
  }

  def index(apis: Seq[RestApi]): Future[BulkDocResponse] = {
    if (null == apis || apis.isEmpty) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val error = validate(apis)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          bulk {
            apis.map(api => {
              api.id = api.generateId()
              indexInto(RestApi.Index / EsConfig.DefaultType).doc(api)
            })
          }.refresh(RefreshPolicy.WAIT_UNTIL)
        }.map(toBulkDocResponse(_))
      }
    }
  }

  def queryApi(query: QueryApi) = {
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.path)) esQueries += wildcardQuery(FieldKeys.FIELD_PATH, query.path + "*")
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      sortFields = Nil
    }
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    EsClient.esClient.execute {
      search(RestApi.Index).query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(sortFields)
        .sourceInclude(defaultIncludeFields :+ FieldKeys.FIELD_PATH :+ FieldKeys.FIELD_METHOD)
    }
  }

  def getByApis(apis: Seq[RestApi]) = {
    if (null == apis || apis.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val error = validate(apis)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
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
      EsClient.esClient.execute {
        delete(id).from(RestApi.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def deleteDoc(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(ids.map(id => delete(id).from(RestApi.Index / EsConfig.DefaultType)))
      }
    }
  }

  def getOne(api: RestApi) = {
    val error = validate(api)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.esClient.execute {
        search(RestApi.Index).query(idsQuery(api.generateId()))
      }
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_IdNonExists.toFutureFail
    } else {
      EsClient.esClient.execute {
        search(RestApi.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  def getById(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(RestApi.Index).query(idsQuery(ids))
      }
    }
  }

  def getAll(project: String) = {
    EsClient.esClient.execute {
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
      val error = validate(api)
      if (null == error) {
        EsClient.esClient.execute {
          update(apiUpdate.id).in(RestApi.Index / EsConfig.DefaultType).doc(api.toUpdateMap)
        }
      } else {
        error.toFutureFail
      }
    }
  }

  def docCount(path: String, project: String) = {
    EsClient.esClient.execute {
      count(RestApi.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_PATH, path),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }
    }
  }

  def docCount(path: String, method: String, version: String, project: String) = {
    EsClient.esClient.execute {
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

  def validate(api: RestApi): ErrorMessage = {
    if (StringUtils.isEmpty(api.path)) {
      ErrorMessages.error_EmptyPath
    } else if (StringUtils.isEmpty(api.method)) {
      ErrorMessages.error_EmptyMethod
    } else if (StringUtils.isEmpty(api.project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.isEmpty(api.group)) {
      ErrorMessages.error_EmptyGroup
    } else {
      null
    }
  }

  def validate(apis: Seq[RestApi]): ErrorMessage = {
    var isOk = true
    val apiSet = mutable.Set[RestApi]()
    var errMsg: ErrorMessage = null
    for (i <- 0 until apis.length if isOk) {
      val api = apis(i)
      if (apiSet.contains(api)) {
        isOk = false
        errMsg = ErrorMessages.error_DuplicateApi(s"duplicate api: ${api.path}:${api.method}")
      } else {
        val error = validate(api)
        if (null == error) {
          apiSet += api
        } else {
          isOk = false
          errMsg = error
        }
      }
    }
    errMsg
  }

  case class ApiUpdate(
                        id: String,
                        api: RestApi
                      )

}
