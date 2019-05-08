package asura.core.job.impl

import asura.common.util.FutureUtils.RichFuture
import asura.core.es.service.{IndexService, JobReportDataHttpService}
import com.typesafe.scalalogging.Logger
import org.quartz.{Job, JobExecutionContext}

import scala.concurrent.duration._

class ClearJobReportDataIndicesJob extends Job {

  import ClearJobReportDataIndicesJob._

  val logger = Logger(classOf[ClearJobReportDataIndicesJob])

  override def execute(context: JobExecutionContext): Unit = {
    val detail = context.getJobDetail
    val day = detail.getJobDataMap.getInt(KEY_DAY)
    if (day > 0) {
      val response = JobReportDataHttpService.getIndices().await(30 seconds)
      if (response.isSuccess) {
        val indices = response.result.slice(day, response.result.size).map(_.index)
        if (indices.nonEmpty) {
          logger.info(s"delete indices: ${indices.mkString(",")}")
          IndexService.delIndex(indices).await(30 seconds)
        }
      } else {
        logger.error(response.error.reason)
      }
    }
  }
}

object ClearJobReportDataIndicesJob {

  val NAME = "ClearJobReportIndicesJob"
  val KEY_CRON = "cron"
  val KEY_DAY = "day"

  val DEFAULT_DAY = 20
}
