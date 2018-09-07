package asura.core.notify

import asura.core.es.model.ReportNotify
import asura.core.job.JobExecDesc

import scala.concurrent.Future

trait JobNotifyFunction {

  /** id for every type of notification mechanism */
  val `type`: String
  val description: String

  def notify(execDesc: JobExecDesc, subscriber: ReportNotify, reportId: String): Future[NotifyResponse]
}
