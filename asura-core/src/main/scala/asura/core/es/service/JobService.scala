package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{FieldKeys, Job}
import asura.core.es.{EsClient, EsConfig}
import asura.core.job.actor.JobStatusActor.JobQueryMessage
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.QueryDefinition

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobService {

  def index(job: Job) = {
    if (null == job) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val (isOK, errMsg) = validate(job)
      if (!isOK) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        val jobId = Job.buildJobKey(job)
        EsClient.httpClient.execute {
          exists(jobId, Job.Index, EsConfig.DefaultType)
        }.flatMap(res => {
          res match {
            case Left(failure) =>
              FutureUtils.requestFail(failure.error.reason)
            case Right(success) =>
              if (success.result) {
                FutureUtils.illegalArgs(s"${job.scheduler}:${job.group}:${job.name} already exists.")
              } else {
                EsClient.httpClient.execute {
                  indexInto(Job.Index / EsConfig.DefaultType).doc(job).id(jobId).refresh(RefreshPolicy.WAIT_UNTIL)
                }
              }
          }
        })
      }
    }
  }


  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(Job.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def deleteDoc(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        bulk(ids.map(id => delete(id).from(Job.Index / EsConfig.DefaultType)))
      }
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(Job.Index).query(idsQuery(id))
      }
    }
  }

  def getById(ids: Seq[String]) = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(Job.Index).query(idsQuery(ids))
      }
    }
  }

  def updateJob(job: Job) = {
    if (null == job) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val (isOk, errMsg) = validate(job)
      if (!isOk) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.httpClient.execute {
          update(Job.buildJobKey(job)).in(Job.Index / EsConfig.DefaultType).doc(JacksonSupport.stringify(job.toUpdateMap))
        }
      }
    }
  }

  def docCount(path: String, project: String) = {
    EsClient.httpClient.execute {
      count(Job.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_PATH, path),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }
    }
  }

  def docCount(path: String, method: String, version: String, project: String) = {
    EsClient.httpClient.execute {
      count(Job.Index).filter {
        boolQuery().must(
          termQuery(FieldKeys.FIELD_PATH, path),
          termQuery(FieldKeys.FIELD_METHOD, method),
          termQuery(FieldKeys.FIELD_VERSION, version),
          termQuery(FieldKeys.FIELD_PROJECT, project)
        )
      }
    }
  }

  def validate(job: Job): BoolErrorRes = {
    if (StringUtils.isEmpty(job.summary)) {
      (false, "Empty job name")
    } else if (StringUtils.isEmpty(job.classAlias)) {
      (false, "Empty job class")
    } else if (StringUtils.isEmpty(job.group)) {
      (false, "Empty job group")
    } else if (StringUtils.isEmpty(job.scheduler)) {
      (false, "Empty job scheduler")
    } else {
      (true, null)
    }
  }


  def geJobById(id: String): Future[Job] = {
    getById(id).map(res => {
      res match {
        case Right(success) =>
          if (success.result.isEmpty) {
            throw IllegalRequestException(s"Api: ${id} not found.")
          } else {
            val hit = success.result.hits.hits(0)
            JacksonSupport.parse(hit.sourceAsString, classOf[Job])
          }
        case Left(failure) =>
          throw RequestFailException(failure.error.reason)
      }
    })
  }

  def query(query: JobQueryMessage) = {
    val queryDefinitions = ArrayBuffer[QueryDefinition]()
    if (StringUtils.isNotEmpty(query.scheduler)) {
      queryDefinitions += termQuery(FieldKeys.FIELD_SCHEDULER, query.scheduler)
    }
    if (StringUtils.isNotEmpty(query.group)) {
      queryDefinitions += termQuery(FieldKeys.FIELD_GROUP, query.group)
    }
    if (StringUtils.isNotEmpty(query.text)) {
      queryDefinitions += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    }
    EsClient.httpClient.execute {
      val clause = search(Job.Index).query {
        boolQuery().must(queryDefinitions)
      }
      if (Option(query.from).isDefined && query.from >= 0) clause.from(query.from)
      if (Option(query.size).isDefined && query.size >= 0) clause.size(query.size)
      clause
    }
  }
}
