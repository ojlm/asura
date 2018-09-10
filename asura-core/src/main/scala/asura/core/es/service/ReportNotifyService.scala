package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
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

object ReportNotifyService extends CommonService {

  val logger = Logger("ReportNotifyService")

  def index(notify: ReportNotify): Future[IndexDocResponse] = {
    EsClient.esClient.execute {
      indexInto(ReportNotify.Index / EsConfig.DefaultType).doc(notify).refresh(RefreshPolicy.WAIT_UNTIL)
    }.map(toIndexDocResponse(_))
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        delete(id).from(ReportNotify.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  // TODO: pagination
  def getSubscribers(jobId: String, enabled: Boolean = true, trigger: String = null): Future[Seq[ReportNotify]] = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(jobId)) esQueries += termQuery(FieldKeys.FIELD_JOB_ID, jobId)
    if (StringUtils.isNotEmpty(trigger)) esQueries += termQuery(FieldKeys.FIELD_TRIGGER, trigger)
    if (Option(enabled).isDefined) esQueries += matchQuery(FieldKeys.FIELD_ENABLED, enabled)
    EsClient.esClient.execute {
      search(ReportNotify.Index)
        .query(boolQuery().must(esQueries))
        .sortByFieldAsc(FieldKeys.FIELD_CREATED_AT)
        .sourceExclude(FieldKeys.FIELD_CREATOR, FieldKeys.FIELD_CREATED_AT)
    }.map { res =>
      if (res.isSuccess) {
        if (res.result.isEmpty) {
          Nil
        } else {
          res.result.hits.hits.map(hit => JacksonSupport.parse(hit.sourceAsString, classOf[ReportNotify]))
        }
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    }
  }

  def notifySubscribers(execDesc: JobExecDesc, reportId: String): Future[NotifyResponses] = {
    val job = execDesc.job
    val report = execDesc.report
    ReportNotifyService.getSubscribers(Job.buildJobKey(job)).flatMap(subscribers => {
      val responses = subscribers.map(subscriber => {
        val func = JobNotifyManager.get(subscriber.`type`)
        if (func.nonEmpty) {
          report.result match {
            case JobExecDesc.STATUS_SUCCESS =>
              if (ReportNotify.TRIGGER_ALL.equals(subscriber.trigger) || ReportNotify.TRIGGER_SUCCESS.equals(subscriber.trigger)) {
                func.get.notify(execDesc, subscriber, reportId).recover {
                  case t: Throwable => NotifyResponse(false, subscriber.subscriber, ErrorMessages.error_Throwable(t))
                }
              } else {
                Future.successful(null)
              }
            case _ =>
              if (ReportNotify.TRIGGER_ALL.equals(subscriber.trigger) || ReportNotify.TRIGGER_FAIL.equals(subscriber.trigger)) {
                func.get.notify(execDesc, subscriber, reportId).recover {
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
}
