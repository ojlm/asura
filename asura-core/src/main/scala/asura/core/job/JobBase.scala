package asura.core.job

import asura.common.exceptions.NotSupportedException
import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.core.es.model.JobData
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

trait JobBase {

  val meta: JobMeta

  def checkJobData(jobData: JobData): BoolErrorRes = (true, ApiMsg.SUCCESS)

  /** this method is inner blocked. */
  def doTest(execDesc: JobExecDesc, log: String => Unit = null): Unit = {
    throw NotSupportedException(s"${meta.classAlias} do not support doTest operation")
  }

  /** this method should not be blocked */
  def doTestAsync(execDesc: JobExecDesc, log: String => Unit = null): Future[JobExecDesc] = {
    throw NotSupportedException(s"${meta.classAlias} do not support doTestAsync operation")
  }
}

object JobBase {
  val logger = Logger("JobBase")
}
