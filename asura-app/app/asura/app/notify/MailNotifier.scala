package asura.app.notify

import asura.core.es.model.JobNotify
import asura.core.job.JobExecDesc
import asura.core.notify.{JobNotifyFunction, NotifyResponse}
import play.api.libs.mailer.MailerClient

import scala.concurrent.Future

case class MailNotifier(mailerClient: MailerClient) extends JobNotifyFunction {

  override val `type`: String = "Email"
  override val description: String =
    """## Email notifier
      |> A `SMTP` client
    """.stripMargin

  override def notify(execDesc: JobExecDesc, subscriber: JobNotify): Future[NotifyResponse] = {
    // TODO
    Future.successful(null)
  }
}
