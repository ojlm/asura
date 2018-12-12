package com.example.asura.notify

import asura.core.es.model.JobNotify
import asura.core.job.JobExecDesc
import asura.core.notify.{JobNotifyFunction, NotifyResponse}
import play.api.Configuration

import scala.concurrent.Future

class ExampleNotification(config: Configuration) extends JobNotifyFunction {

  /** id for every type of notification mechanism */
  override val `type`: String = "SmsNotification"
  override val description: String =
    """# SmsNotification
      |markdown syntax
    """.stripMargin

  override def notify(execDesc: JobExecDesc, subscriber: JobNotify): Future[NotifyResponse] = {
    Future.successful(NotifyResponse(true, subscriber.subscriber))
  }
}

