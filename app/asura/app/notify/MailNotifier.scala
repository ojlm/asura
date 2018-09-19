package asura.app.notify

import asura.core.es.model.ReportNotify
import asura.core.job.JobExecDesc
import asura.core.notify.{JobNotifyFunction, JobNotifyManager, NotifyResponse}
import play.api.libs.mailer.MailerClient

import scala.concurrent.Future

case class MailNotifier(mailerClient: MailerClient) extends JobNotifyFunction {

  JobNotifyManager.register(this)
  override val `type`: String = "mail"
  override val description: String = "mail notifier"

  override def notify(execDesc: JobExecDesc, subscriber: ReportNotify, reportId: String): Future[NotifyResponse] = {
    // TODO
    Future.successful(null)
  }
}
