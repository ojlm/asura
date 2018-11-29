package asura.core.job.impl

import asura.common.util.FutureUtils.RichFuture
import asura.core.es.service.{DomainOnlineLogService, IndexService, OnlineRequestLogService, RestApiOnlineLogService}
import com.typesafe.scalalogging.Logger
import org.quartz.{Job, JobExecutionContext}

import scala.concurrent.duration._

class SyncOnlineDomainAndRestApiJob extends Job {

  import SyncOnlineDomainAndRestApiJob._

  val logger = Logger(classOf[SyncOnlineDomainAndRestApiJob])

  override def execute(context: JobExecutionContext): Unit = {
    val detail = context.getJobDetail
    val dayCount = detail.getJobDataMap.getInt(KEY_DAY)
    val domainCount = detail.getJobDataMap.getInt(KEY_DOMAIN_COUNT)
    val apiCount = detail.getJobDataMap.getInt(KEY_API_COUNT)
    if (domainCount > 0) {
      val logs = DomainOnlineLogService.syncOnlineDomain(domainCount).await
      logs.foreach(domainCountLog => {
        val apiLogs = OnlineRequestLogService.getOnlineApi(domainCountLog.name, apiCount).await
        RestApiOnlineLogService.index(apiLogs, domainCountLog.date).await
      })
    }
    if (dayCount > 0) {
      deleteOutdatedIndices(dayCount)
    }
  }

  def deleteOutdatedIndices(dayCount: Int): Unit = {
    val response = RestApiOnlineLogService.getIndices().await(30 seconds)
    if (response.isSuccess) {
      val indices = response.result.slice(dayCount, response.result.size).map(_.index)
      if (indices.nonEmpty) {
        logger.info(s"delete indices: ${indices.mkString(",")}")
        IndexService.delIndex(indices).await(30 seconds)
      }
    } else {
      logger.error(response.error.reason)
    }
  }

  def calculateCoverage() = {
    // TODO
  }
}

object SyncOnlineDomainAndRestApiJob {

  val NAME = "SyncOnlineDomainAndRestApiJob"
  val KEY_CRON = "cron"
  val KEY_DAY = "day"
  val KEY_DOMAIN_COUNT = "domainCount"
  val KEY_API_COUNT = "apiCount"
}
