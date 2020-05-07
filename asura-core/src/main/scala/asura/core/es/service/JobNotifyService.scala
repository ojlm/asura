package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.{LogUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.job.JobExecDesc
import asura.core.model.QueryJobNotify
import asura.core.notify.{JobNotifyManager, NotifyResponse, NotifyResponses}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object JobNotifyService extends CommonService {

  val logger = Logger("ReportNotifyService")

  def index(notify: JobNotify): Future[IndexDocResponse] = {
    val errorMsg = validate(notify)
    if (null == errorMsg) {
      EsClient.esClient.execute {
        indexInto(JobNotify.Index).doc(notify).refresh(RefreshPolicy.WAIT_FOR)
      }.map(toIndexDocResponse(_))
    } else {
      errorMsg.toFutureFail
    }
  }

  def index(notifies: Seq[JobNotify]): Future[BulkDocResponse] = {
    var errorMsg: ErrorMessage = null
    val ret = notifies.forall(item => {
      errorMsg = validate(item, false)
      errorMsg == null
    })
    if (ret) {
      EsClient.esClient.execute {
        bulk {
          notifies.map(notify => indexInto(JobNotify.Index).doc(notify))
        }.waitForRefresh
      }.map(toBulkDocResponse(_))
    } else {
      errorMsg.toFutureFail
    }
  }

  def updateNotify(id: String, notify: JobNotify): Future[UpdateDocResponse] = {
    val errorMsg = validate(notify)
    if (null == errorMsg) {
      EsClient.esClient.execute {
        update(id).in(JobNotify.Index).doc(notify.toUpdateMap).refresh(RefreshPolicy.WAIT_FOR)
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
        delete(id).from(JobNotify.Index).refresh(RefreshPolicy.WAIT_FOR)
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
    EsClient.esClient.execute {
      search(JobNotify.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .searchAfter(if (null != query.sort) query.sort else Nil)
        .sortBy(FieldSort(FieldKeys.FIELD_CREATED_AT).asc(), FieldSort(FieldKeys.FIELD__ID).desc())
        .sourceExclude(FieldKeys.FIELD_CREATOR, FieldKeys.FIELD_CREATED_AT, FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION)
    }
  }

  def getJobSubscribers(jobId: String, size: Int, sort: Seq[Any] = Nil): Future[(Long, Seq[Any], Seq[JobNotify])] = {
    val query = QueryJobNotify(jobId = jobId)
    query.size = size
    query.sort = sort
    querySubscribers(query).map { res =>
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          (0L, Nil, Nil)
        } else {
          var sort: Seq[Any] = Nil
          val hits = res.result.hits.hits
          val total = res.result.totalHits
          val docs = for (i <- 0 until hits.length) yield {
            val hit = hits(i)
            if (i == hits.length - 1) {
              sort = hit.sort.getOrElse(Nil)
            }
            JacksonSupport.parse(hit.sourceAsString, classOf[JobNotify])
          }
          (total, sort, docs)
        }
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    }
  }

  def notifySubscribers(execDesc: JobExecDesc): Future[NotifyResponses] = {
    val responses = ArrayBuffer[NotifyResponse]()
    var leftLoop = 0
    var afterSort: Seq[Any] = Nil
    var initResponse: Future[Seq[NotifyResponse]] = Future.successful(Nil)
    JobNotifyService.getJobSubscribers(execDesc.jobId, EsConfig.MaxCount).map(sortAndSubscribers => {
      val (total, sort, subscribers) = sortAndSubscribers
      if (subscribers.nonEmpty) {
        initResponse = Future.sequence(sendNotifications(execDesc, subscribers))
        if (total > EsConfig.MaxCount) {
          afterSort = sort
          leftLoop = Math.ceil((total - EsConfig.MaxCount).toDouble / EsConfig.MaxCount.toDouble).toInt
        }
      }
    })
    if (leftLoop > 0) {
      1.to(leftLoop).foldLeft(initResponse)((seqFutureResponse, _) => {
        for {
          prevResponse <- seqFutureResponse
          sortAndSubscribers <- JobNotifyService.getJobSubscribers(execDesc.jobId, EsConfig.MaxCount, afterSort)
          currSeqResponses <- {
            if (prevResponse.nonEmpty) responses ++= prevResponse
            val (_, sort, subscribers) = sortAndSubscribers
            afterSort = sort
            Future.sequence(sendNotifications(execDesc, subscribers)).recover {
              case t: Throwable =>
                logger.error(LogUtils.stackTraceToString(t))
                Nil
            }
          }
        } yield currSeqResponses
      }).map(lastSeqFutureResponse => {
        responses ++= lastSeqFutureResponse
        NotifyResponses(responses)
      })
    } else {
      initResponse.map(NotifyResponses(_))
    }
  }

  def sendNotifications(execDesc: JobExecDesc, notifies: Seq[JobNotify]) = {
    val report = execDesc.report
    notifies.filter(_.enabled).map(subscriber => {
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
  }

  def validate(notify: JobNotify, checkJobId: Boolean = true): ErrorMessage = {
    if (StringUtils.isEmpty(notify.trigger)) {
      notify.trigger = JobNotify.TRIGGER_ALL
    }
    if (Option(notify.enabled).isEmpty) {
      notify.enabled = true
    }
    if (checkJobId && StringUtils.isEmpty(notify.jobId)) {
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
