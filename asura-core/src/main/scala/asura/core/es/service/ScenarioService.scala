package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.{DateUtils, JsonUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsResponse}
import asura.core.model.QueryScenario
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl.{bulk, delete, indexInto, _}
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.searches.queries.{NestedQuery, Query}
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.collection.{Iterable, mutable}
import scala.concurrent.Future

object ScenarioService extends CommonService {

  val basicFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_LABELS,
  )

  def index(s: Scenario): Future[IndexDocResponse] = {
    val error = check(s)
    if (null == error) {
      EsClient.esClient.execute {
        indexInto(Scenario.Index).doc(s).refresh(RefreshPolicy.WAIT_FOR)
      }.map(toIndexDocResponse(_))
    } else {
      error.toFutureFail
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    EsClient.esClient.execute {
      delete(id).from(Scenario.Index).refresh(RefreshPolicy.WAIT_FOR)
    }.map(toDeleteDocResponse(_))
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    EsClient.esClient.execute {
      bulk(ids.map(id => delete(id).from(Scenario.Index)))
    }.map(toBulkDocResponse(_))
  }

  def getById(id: String) = {
    EsClient.esClient.execute {
      search(Scenario.Index).query(idsQuery(id)).size(1).sourceExclude(defaultExcludeFields)
    }
  }

  def copyById(id: String, creator: String) = {
    getScenarioById(id).flatMap(scenario => {
      val httpSeq = ArrayBuffer[String]()
      val dubboSeq = ArrayBuffer[String]()
      val sqlSeq = ArrayBuffer[String]()
      scenario.steps.foreach(step => {
        step.`type` match {
          case ScenarioStep.TYPE_HTTP => httpSeq += step.id
          case ScenarioStep.TYPE_DUBBO => dubboSeq += step.id
          case ScenarioStep.TYPE_SQL => sqlSeq += step.id
          case _ =>
        }
      })
      val res = for {
        cs <- HttpCaseRequestService.getByIdsAsMap(httpSeq.toSeq)
        dubbo <- DubboRequestService.getByIdsAsMap(dubboSeq.toSeq)
        sql <- SqlRequestService.getByIdsAsMap(sqlSeq.toSeq)
      } yield (cs, dubbo, sql)
      val idxMap = mutable.HashMap[Int, Int]()
      res.flatMap(triple => {
        val requests = ArrayBuffer[IndexRequest]()
        val now = DateUtils.nowDateTime
        for (i <- 0.until(scenario.steps.length)) {
          val step = scenario.steps(i)
          step.`type` match {
            case ScenarioStep.TYPE_HTTP =>
              val doc = triple._1(step.id)
              doc.fillCommonFields(creator, now)
              doc.copyFrom = step.id
              requests += indexInto(HttpCaseRequest.Index).doc(doc)
              idxMap(i) = requests.length - 1
            case ScenarioStep.TYPE_DUBBO =>
              val doc = triple._2(step.id)
              doc.fillCommonFields(creator, now)
              doc.copyFrom = step.id
              requests += indexInto(DubboRequest.Index).doc(doc)
              idxMap(i) = requests.length - 1
            case ScenarioStep.TYPE_SQL =>
              val doc = triple._3(step.id)
              doc.fillCommonFields(creator, now)
              doc.copyFrom = step.id
              requests += indexInto(SqlRequest.Index).doc(doc)
              idxMap(i) = requests.length - 1
            case _ =>
          }
        }
        EsClient.esClient.execute {
          bulk(requests).refresh(RefreshPolicy.WAIT_FOR)
        }.flatMap(response => {
          if (response.isSuccess) {
            for (i <- 0.until(scenario.steps.length)) {
              val step = scenario.steps(i)
              step.`type` match {
                case ScenarioStep.TYPE_HTTP | ScenarioStep.TYPE_DUBBO | ScenarioStep.TYPE_SQL =>
                  val bulkItem = response.result.items(idxMap(i))
                  step.id = bulkItem.id
                case _ =>
              }
            }
            scenario.fillCommonFields(creator, now)
            EsClient.esClient.execute {
              indexInto(Scenario.Index).doc(scenario).refresh(RefreshPolicy.WAIT_FOR)
            }.map(toIndexDocResponse(_))
          } else {
            throw ErrorMessages.error_EsRequestFail(response).toException
          }
        })
      })
    })
  }

  def getScenarioById(id: String): Future[Scenario] = {
    getById(id).map(res => {
      if (res.isSuccess && res.result.nonEmpty) {
        JacksonSupport.parse(res.result.hits.hits(0).sourceAsString, classOf[Scenario])
      } else {
        throw ErrorMessages.error_IdNonExists.toException
      }
    })
  }

  def getRelativesById(id: String): Future[Map[String, Any]] = {
    getById(id).flatMap(response => {
      if (response.isSuccess) {
        if (response.result.nonEmpty) {
          val scenarioDoc = EsResponse.toSingleApiData(response.result, true)
          val steps = scenarioDoc.getOrElse(FieldKeys.FIELD_STEPS, Nil).asInstanceOf[Seq[Map[String, Any]]]
          val httpSeq = ArrayBuffer[String]()
          val dubboSeq = ArrayBuffer[String]()
          val sqlSeq = ArrayBuffer[String]()
          steps.foreach(step => {
            val ty = step.getOrElse(FieldKeys.FIELD_TYPE, null).asInstanceOf[String]
            val id = step.getOrElse(FieldKeys.FIELD_ID, null).asInstanceOf[String]
            ty match {
              case ScenarioStep.TYPE_HTTP => httpSeq += id
              case ScenarioStep.TYPE_DUBBO => dubboSeq += id
              case ScenarioStep.TYPE_SQL => sqlSeq += id
              case _ =>
            }
          })
          val res = for {
            cs <- HttpCaseRequestService.getByIdsAsMap(httpSeq.toSeq, true)
            dubbo <- DubboRequestService.getByIdsAsMap(dubboSeq.toSeq, true)
            sql <- SqlRequestService.getByIdsAsMap(sqlSeq.toSeq, true)
          } yield (cs, dubbo, sql)
          res.map(triple => {
            Map("scenario" -> scenarioDoc, "case" -> triple._1, "dubbo" -> triple._2, "sql" -> triple._3)
          })
        } else {
          ErrorMessages.error_IdNonExists.toFutureFail
        }
      } else {
        throw ErrorMessages.error_EsRequestFail(response).toException
      }
    })
  }

  def getByIds(ids: Seq[String]) = {
    EsClient.esClient.execute {
      search(Scenario.Index)
        .query(idsQuery(ids))
        .size(ids.length)
        .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
        .sourceExclude(defaultExcludeFields)
    }
  }

  def updateScenario(id: String, s: Scenario): Future[UpdateDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(id).in(Scenario.Index).doc(JsonUtils.stringify(s.toUpdateMap))
      }.map(toUpdateDocResponse(_))
    }
  }

  /**
   * Seq({id->scenario})
   */
  def getScenariosByIds(ids: Seq[String]): Future[Seq[(String, Scenario)]] = {
    getByIds(ids).map(res => {
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          Nil
        } else {
          res.result.hits.hits.toIndexedSeq.map(hit => (hit.id, JacksonSupport.parse(hit.sourceAsString, classOf[Scenario])))
        }
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    })
  }

  def getScenariosByIdsAsMap(ids: Seq[String]): Future[Map[String, Scenario]] = {
    getByIds(ids).map(res => {
      val map = mutable.Map[String, Scenario]()
      if (res.isSuccess) {
        if (res.result.nonEmpty) {
          res.result.hits.hits.foreach(hit =>
            map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[Scenario]))
          )
        }
        map.toMap
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    })
  }

  def queryScenario(query: QueryScenario) = {
    if (null != query.ids && query.ids.nonEmpty) {
      getByIds(query.ids)
    } else {
      var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
      val esQueries = ArrayBuffer[Query]()
      if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
      if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
      if (StringUtils.isNotEmpty(query.text)) {
        esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
        sortFields = Nil
      }
      EsClient.esClient.execute {
        search(Scenario.Index).query(boolQuery().must(esQueries))
          .from(query.pageFrom)
          .size(query.pageSize)
          .sortBy(sortFields)
      }
    }
  }

  def check(s: Scenario): ErrorMessage = {
    if (null == s) {
      ErrorMessages.error_EmptyScenario
    } else if (StringUtils.isEmpty(s.summary)) {
      ErrorMessages.error_EmptySummary
    } else if (StringUtils.isEmpty(s.project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.isEmpty(s.group)) {
      ErrorMessages.error_EmptyGroup
    } else {
      null
    }
  }

  def containSteps(docsIds: Seq[String], stepType: String) = {
    val query = NestedQuery(FieldKeys.FIELD_STEPS, boolQuery().must(
      termsQuery(FieldKeys.FIELD_NESTED_STEPS_ID, docsIds),
      termQuery(FieldKeys.FIELD_NESTED_STEPS_TYPE, stepType)
    ))
    EsClient.esClient.execute {
      search(Scenario.Index).query(query)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields)
    }
  }

  def containEnv(ids: Seq[String]) = {
    val query = boolQuery().must(termsQuery(FieldKeys.FIELD_ENV, ids))
    EsClient.esClient.execute {
      search(Scenario.Index).query(query)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceInclude(defaultIncludeFields)
    }
  }

  def getByIdsAsRawMap(ids: Iterable[String]) = {
    if (null != ids && ids.nonEmpty) {
      EsClient.esClient.execute {
        search(Scenario.Index).query(idsQuery(ids)).size(ids.size).sourceInclude(basicFields)
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }
}
