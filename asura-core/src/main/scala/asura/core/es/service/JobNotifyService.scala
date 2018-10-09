package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.QueryJobNotify
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.job.JobExecDesc
import asura.core.notify.{JobNotifyManager, NotifyResponse, NotifyResponses}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobNotifyService extends CommonService {

  val logger = Logger("ReportNotifyService")

  def index(notify: JobNotify): Future[IndexDocResponse] = {
    val errorMsg = validate(notify)
    if (null == errorMsg) {
      EsClient.esClient.execute {
        indexInto(JobNotify.Index / EsConfig.DefaultType).doc(notify).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    } else {
      errorMsg.toFutureFail
    }
  }

  def index(notifies: Seq[JobNotify]): Future[BulkDocResponse] = {
    var errorMsg: ErrorMessages.ErrorMessage = null
    val ret = notifies.forall(item => {
      errorMsg = validate(item)
      errorMsg == null
    })
    if (ret) {
      EsClient.esClient.execute {
        bulk {
          notifies.map(notify => indexInto(JobNotify.Index / EsConfig.DefaultType).doc(notify))
        }
      }.map(toBulkDocResponse(_))
    } else {
      errorMsg.toFutureFail
    }
  }

  def updateNotify(id: String, notify: JobNotify): Future[UpdateDocResponse] = {
    val errorMsg = validate(notify)
    if (null == errorMsg) {
      EsClient.esClient.execute {
        update(id).in(JobNotify.Index / EsConfig.DefaultType).doc(notify.toUpdateMap).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toUpdateDocResponse(_))
    } else {
      errorMsg.toFutureFail
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        delete(id).from(JobNotify.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def querySubscribers(query: QueryJobNotify) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.jobId)) esQueries += termQuery(FieldKeys.FIELD_JOB_ID, query.jobId)
    if (StringUtils.isNotEmpty(query.subscriber)) esQueries += wildcardQuery(FieldKeys.FIELD_SUBSCRIBER, query.subscriber + "*")
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    if (StringUtils.isNotEmpty(query.trigger)) esQueries += termQuery(FieldKeys.FIELD_TRIGGER, query.trigger)
    if (Option(query.enabled).isDefined) {
      esQueries += termQuery(FieldKeys.FIELD_ENABLED, query.enabled)
    } else {
      esQueries += termQuery(FieldKeys.FIELD_ENABLED, true)
    }
    EsClient.esClient.execute {
      search(JobNotify.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceExclude(FieldKeys.FIELD_CREATOR, FieldKeys.FIELD_CREATED_AT, FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION)
    }
  }

  def getAllJobSubscribers(jobId: String): Future[Seq[JobNotify]] = {
    val query = QueryJobNotify(jobId = jobId)
    query.size = Integer.MAX_VALUE
    querySubscribers(query).map { res =>
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          Nil
        } else {
          res.result.hits.hits.map(hit => JacksonSupport.parse(hit.sourceAsString, classOf[JobNotify]))
        }
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    }
  }

  def notifySubscribers(execDesc: JobExecDesc): Future[NotifyResponses] = {
    val report = execDesc.report
    JobNotifyService.getAllJobSubscribers(execDesc.jobId).flatMap(subscribers => {
      val responses = subscribers.map(subscriber => {
        val func = JobNotifyManager.get(subscriber.`type`)
        if (func.nonEmpty) {
          report.result match {
            case JobExecDesc.STATUS_SUCCESS =>
              if (JobNotify.TRIGGER_ALL.equals(subscriber.trigger) || JobNotify.TRIGGER_SUCCESS.equals(subscriber.trigger)) {
                func.get.notify(execDesc, subscriber).recover {
                  case t: Throwable => NotifyResponse(false, subscriber.subscriber, ErrorMessages.error_Throwable(t))
                }
              } else {
                Future.successful(null)
              }
            case _ =>
              if (JobNotify.TRIGGER_ALL.equals(subscriber.trigger) || JobNotify.TRIGGER_FAIL.equals(subscriber.trigger)) {
                func.get.notify(execDesc, subscriber).recover {
                  case t: Throwable => NotifyResponse(false, subscriber.subscriber, ErrorMessages.error_Throwable(t))
                }
              } else {
                Future.successful(null)
              }
          }
        } else {
          Future.successful(NotifyResponse(false, subscriber.subscriber, ErrorMessages.error_NoNotifyImplementation(subscriber.`type`)))
        }
      })
      Future.sequence(responses).map(responses => NotifyResponses(responses))
    })
  }

  def validate(notify: JobNotify): ErrorMessages.ErrorMessage = {
    if (StringUtils.isEmpty(notify.trigger)) {
      notify.trigger = JobNotify.TRIGGER_ALL
    }
    if (Option(notify.enabled).isEmpty) {
      notify.enabled = true
    }
    if (StringUtils.isEmpty(notify.jobId)) {
      ErrorMessages.error_EmptyJobId
    } else if (StringUtils.isEmpty(notify.group)) {
      ErrorMessages.error_EmptyGroup
    } else if (StringUtils.isEmpty(notify.project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.isEmpty(notify.subscriber)) {
      ErrorMessages.error_EmptySubscriber
    } else if (StringUtils.isEmpty(notify.`type`)) {
      ErrorMessages.error_EmptyNotifyType
    } else {
      null
    }
  }
}
