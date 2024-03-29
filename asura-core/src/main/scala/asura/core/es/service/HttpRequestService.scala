package asura.core.es.service

import asura.common.exceptions.RequestFailException
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.JobData.JobDataExt
import asura.core.es.model._
import asura.core.es.service.BaseAggregationService._
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.http.HttpValidator
import asura.core.model.BatchOperation.{BatchOperationLabels, BatchTransfer}
import asura.core.model._
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl.{bulk, delete, indexInto, nestedQuery, _}
import com.sksamuel.elastic4s.searches.DateHistogramInterval
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Iterable, mutable}
import scala.concurrent.{ExecutionContext, Future}

object HttpRequestService extends CommonService with BaseAggregationService {

  val basicFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_LABELS,
    FieldKeys.FIELD_OBJECT_REQUEST_HOST,
    FieldKeys.FIELD_OBJECT_REQUEST_URLPATH,
    FieldKeys.FIELD_OBJECT_REQUEST_METHOD,
  )
  val queryFields = basicFields ++ Seq(
    FieldKeys.FIELD_EXPORTS, FieldKeys.FIELD_COPY_FROM
  )

  def index(cs: HttpStepRequest): Future[IndexDocResponse] = {
    val error = HttpValidator.check(cs)
    if (null == error) {
      cs.calcGeneratorCount()
      EsClient.esClient.execute {
        indexInto(HttpStepRequest.Index / EsConfig.DefaultType).doc(cs).refresh(RefreshPolicy.WaitFor)
      }.map(toIndexDocResponse(_))
    } else {
      error.toFutureFail
    }
  }

  def index(css: Seq[HttpStepRequest]): Future[BulkDocResponse] = {
    val error = HttpValidator.check(css)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.esClient.execute {
        bulk(
          css.map(cs => {
            cs.calcGeneratorCount()
            indexInto(HttpStepRequest.Index / EsConfig.DefaultType).doc(cs)
          })
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    EsClient.esClient.execute {
      delete(id).from(HttpStepRequest.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WaitFor)
    }.map(toDeleteDocResponse(_))
  }

  def deleteDoc(ids: Seq[String]): Future[DeleteDocResponse] = {
    EsClient.esClient.execute {
      bulk(ids.map(id => delete(id).from(HttpStepRequest.Index / EsConfig.DefaultType))).refresh(RefreshPolicy.WaitFor)
    }.map(toDeleteDocResponseFromBulk(_))
  }

  def getById(id: String) = {
    EsClient.esClient.execute {
      search(HttpStepRequest.Index).query(idsQuery(id)).size(1)
    }
  }

  def getRequestById(id: String): Future[HttpStepRequest] = {
    EsClient.esClient.execute {
      search(HttpStepRequest.Index).query(idsQuery(id)).size(1)
    }.map(res => {
      if (res.isSuccess && res.result.nonEmpty) {
        JacksonSupport.parse(res.result.hits.hits(0).sourceAsString, classOf[HttpStepRequest])
      } else {
        null
      }
    })
  }

  private def getByIds(ids: Seq[String], filterFields: Boolean = false) = {
    if (null != ids) {
      EsClient.esClient.execute {
        search(HttpStepRequest.Index)
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

  def updateCs(id: String, cs: HttpStepRequest): Future[UpdateDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = HttpValidator.check(cs)
      if (null == error) {
        cs.calcGeneratorCount()
        EsClient.esClient.execute {
          val (src, params) = cs.toUpdateScriptParams
          update(id).in(HttpStepRequest.Index / EsConfig.DefaultType).script {
            script(src).params(params)
          }
        }.map(toUpdateDocResponse(_))
      } else {
        error.toFutureFail
      }
    }
  }

  /**
   * Seq({id->case})
   *
   * @param filterFields if false return all fields of doc, other only return filed in [[queryFields]]
   */
  def getCasesByIds(ids: Seq[String], filterFields: Boolean = false)(implicit executor: ExecutionContext): Future[Seq[(String, HttpStepRequest)]] = {
    if (null != ids && ids.nonEmpty) {
      getByIds(ids, filterFields).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdsNotFound(ids).toException
          } else {
            res.result.hits.hits.toIndexedSeq.map(hit => (hit.id, JacksonSupport.parse(hit.sourceAsString, classOf[HttpStepRequest])))
          }
        } else {
          throw RequestFailException(res.error.reason)
        }
      })
    } else {
      Future.successful(Nil)
    }
  }

  def getByIdsAsMap(ids: Seq[String], filterFields: Boolean = false): Future[Map[String, HttpStepRequest]] = {
    if (null != ids && ids.nonEmpty) {
      val map = mutable.HashMap[String, HttpStepRequest]()
      getByIds(ids, filterFields).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdsNotFound(ids).toException
          } else {
            res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[HttpStepRequest])))
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
        search(HttpStepRequest.Index).query(idsQuery(ids)).size(ids.size).sourceInclude(basicFields)
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  def getCasesByJobDataExtAsMap(group: String, project: String, ext: JobDataExt): Future[Map[String, HttpStepRequest]] = {
    if (null != ext && StringUtils.isNotEmpty(group) && StringUtils.isNotEmpty(project)) {
      val map = mutable.HashMap[String, HttpStepRequest]()
      val esQueries = ArrayBuffer[Query]()
      if (StringUtils.isNotEmpty(group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, group)
      if (StringUtils.isNotEmpty(project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, project)
      if (StringUtils.isNotEmpty(ext.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, ext.text)
      if (StringUtils.isNotEmpty(ext.path)) esQueries += wildcardQuery(FieldKeys.FIELD_OBJECT_REQUEST_URLPATH, s"${ext.path}*")
      if (null != ext.methods && ext.methods.nonEmpty) esQueries += termsQuery(FieldKeys.FIELD_OBJECT_REQUEST_METHOD, ext.methods)
      if (null != ext.labels && ext.labels.nonEmpty) esQueries += nestedQuery(FieldKeys.FIELD_LABELS, termsQuery(FieldKeys.FIELD_NESTED_LABELS_NAME, ext.labels))
      EsClient.esClient.execute {
        search(HttpStepRequest.Index).query(boolQuery().must(esQueries))
          .size(EsConfig.MaxCount)
          .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
      }.map(res => {
        if (res.isSuccess) {
          res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[HttpStepRequest])))
          map.toMap
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    } else {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    }
  }

  /**
   * return Map("total" -> total , "list" -> list), used by api action
   */
  def queryCase(query: QueryCase): Future[Map[String, Any]] = {
    if (null != query.ids && query.ids.nonEmpty) {
      getByIds(query.ids, true).flatMap(res => {
        if (res.isSuccess) {
          val idMap = scala.collection.mutable.HashMap[String, Any]()
          val userIds = mutable.HashSet[String]()
          res.result.hits.hits.foreach(hit => {
            val sourceMap = hit.sourceAsMap
            userIds += sourceMap.getOrElse(FieldKeys.FIELD_CREATOR, StringUtils.EMPTY).asInstanceOf[String]
            idMap += (hit.id -> (sourceMap + (FieldKeys.FIELD__ID -> hit.id)))
          })
          if (userIds.nonEmpty && query.hasCreators) {
            UserProfileService.getByIdsAsRawMap(userIds).map(profiles => {
              Map(
                "total" -> res.result.hits.total,
                "list" -> query.ids.filter(idMap.contains(_)).map(idMap(_)),
                "creators" -> profiles
              )
            })
          } else {
            Future.successful {
              Map("total" -> res.result.hits.total, "list" -> query.ids.filter(idMap.contains(_)).map(idMap(_)))
            }
          }
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    } else {
      var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
      val esQueries = ArrayBuffer[Query]()
      if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
      if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
      if (StringUtils.isNotEmpty(query.text)) {
        esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
        sortFields = Nil
      }
      if (query.isCloned) {
        esQueries += existsQuery(FieldKeys.FIELD_COPY_FROM)
      } else {
        esQueries += boolQuery().not(existsQuery(FieldKeys.FIELD_COPY_FROM))
      }
      if (StringUtils.isNotEmpty(query.host)) esQueries += termQuery(FieldKeys.FIELD_OBJECT_REQUEST_HOST, query.host)
      if (StringUtils.isNotEmpty(query.path)) esQueries += wildcardQuery(FieldKeys.FIELD_OBJECT_REQUEST_URLPATH, s"${query.path}*")
      if (null != query.methods && query.methods.nonEmpty) esQueries += termsQuery(FieldKeys.FIELD_OBJECT_REQUEST_METHOD, query.methods)
      if (null != query.labels && query.labels.nonEmpty) esQueries += nestedQuery(FieldKeys.FIELD_LABELS, termsQuery(FieldKeys.FIELD_NESTED_LABELS_NAME, query.labels))
      EsClient.esClient.execute {
        search(HttpStepRequest.Index).query(boolQuery().must(esQueries))
          .from(query.pageFrom)
          .size(query.pageSize)
          .sortBy(sortFields)
          .sourceInclude(queryFields)
      }.flatMap(res => {
        if (res.isSuccess) {
          if (query.hasCreators) {
            fetchWithCreatorProfiles(res)
          } else {
            Future.successful(EsResponse.toApiData(res.result, true))
          }
        } else {
          ErrorMessages.error_EsRequestFail(res).toFutureFail
        }
      })
    }
  }

  def searchAfter(query: SearchAfterCase) = {
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.creator)) esQueries += termQuery(FieldKeys.FIELD_CREATOR, query.creator)
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      sortFields = Seq(FieldSort(FieldKeys.FIELD__SCORE).desc())
    }
    EsClient.esClient.execute {
      search(HttpStepRequest.Index)
        .query(boolQuery().must(esQueries))
        .size(query.pageSize)
        .searchAfter(query.toSearchAfterSort)
        .sortBy(sortFields)
        .sourceInclude(queryFields)
    }.flatMap { res =>
      if (res.isSuccess) {
        fetchWithCreatorProfiles(res)
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    }
  }

  // note this is not always accurate
  def aroundAggs(aggs: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(aggs, false)
    val aggField = aggs.aggField()
    EsClient.esClient.execute {
      search(HttpStepRequest.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(termsAgg(aggsTermsName, aggField).size(aggs.pageSize()))
    }.map(toAggItems(_, aggField, null))
  }

  def trend(query: AggsQuery): Future[Seq[AggsItem]] = {
    val esQueries = buildEsQueryFromAggQuery(query, false)
    val termsField = query.aggTermsField()
    val dateHistogram = dateHistogramAgg(aggsTermsName, FieldKeys.FIELD_CREATED_AT)
      .interval(DateHistogramInterval.fromString(query.aggInterval()))
      .format("yyyy-MM-dd")
      .subAggregations(termsAgg(aggsTermsName, termsField).size(query.pageSize()))
    EsClient.esClient.execute {
      search(HttpStepRequest.Index)
        .query(boolQuery().must(esQueries))
        .size(0)
        .aggregations(dateHistogram)
    }.map(toAggItems(_, null, termsField))
  }

  def containEnv(ids: Seq[String]) = {
    val query = boolQuery().must(termsQuery(FieldKeys.FIELD_ENV, ids))
    EsClient.esClient.execute {
      search(HttpStepRequest.Index).query(query)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields)
    }
  }

  def batchUpdateLabels(batch: BatchOperationLabels): Future[BulkDocResponse] = {
    if (null != batch.labels && batch.labels.nonEmpty) {
      EsClient.esClient.execute(bulk {
        batch.labels.filter(item => null != item.labels).map(item => {
          val labels = item.labels.map(label => Map(FieldKeys.FIELD_NAME -> label.name))
          update(item.id).in(HttpStepRequest.Index / EsConfig.DefaultType).doc(Map(FieldKeys.FIELD_LABELS -> labels))
        })
      }.refresh(RefreshPolicy.WaitFor)).map(toBulkDocResponse(_))
    } else {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    }
  }

  def batchTransfer(batch: BatchTransfer): Future[BulkDocResponse] = {
    if (StringUtils.isNotEmpty(batch.group) && StringUtils.isNotEmpty(batch.project) && null != batch.ids && batch.ids.nonEmpty) {
      EsClient.esClient.execute(bulk {
        batch.ids.map(csId => {
          val docMap = Map(
            FieldKeys.FIELD_GROUP -> batch.group,
            FieldKeys.FIELD_PROJECT -> batch.project,
            FieldKeys.FIELD_ENV -> StringUtils.EMPTY
          )
          update(csId).in(HttpStepRequest.Index / EsConfig.DefaultType).doc(docMap)
        })
      }.refresh(RefreshPolicy.WaitFor)).map(toBulkDocResponse(_))
    } else {
      ErrorMessages.error_InvalidRequestParameters.toFutureFail
    }
  }

  // assume the api is less equal than twice count of online api
  // return api set of specified project, key: {method}{urlPath} value: count
  def getApiSet(project: Project, apisQuery: Seq[Query], aggSize: Int): Future[mutable.HashMap[String, Long]] = {
    val apiSet = mutable.HashMap[String, Long]()
    EsClient.esClient.execute {
      search(HttpStepRequest.Index)
        .query(boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, project.group),
          termQuery(FieldKeys.FIELD_PROJECT, project.id),
          // boolQuery().should(apisQuery)
        ))
        .size(0)
        .aggregations(
          termsAgg(aggsTermsName, FieldKeys.FIELD_OBJECT_REQUEST_URLPATH).size(aggSize)
            .subAggregations(termsAgg(aggsTermsName, FieldKeys.FIELD_OBJECT_REQUEST_METHOD))
        )
    }.map(toAggItems(_, null, FieldKeys.FIELD_METHOD))
      .map(items => {
        items.foreach(apiItem => {
          apiItem.sub.foreach(methodItem => apiSet += (s"${methodItem.id}${apiItem.id}" -> apiItem.count))
        })
        apiSet
      })
  }
}
