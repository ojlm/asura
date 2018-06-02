package asura.app.jobs

import asura.core.job._

import scala.concurrent.Future

object EchoHelloJob extends JobBase {

  override val meta = JobMeta(
    name = "Echo Hello",
    desc = "Echo hello data ~",
    classAlias = "EchoHelloJob"
  )

  override def doTest(execDesc: JobExecDesc, log: String => Unit = null): Unit = {
    val job = execDesc.job
    val data = if (null != job && null != job.jobData && null != job.jobData.ext) {
      job.jobData.ext.getOrElse("data", "world").asInstanceOf[String]
    } else {
      "world"
    }
    if (null != log) log(data)
  }

  override def doTestAsync(execDesc: JobExecDesc, log: String => Unit): Future[JobExecDesc] = {
    doTest(execDesc, log)
    Future.successful(execDesc)
  }

}

class EchoHelloJob extends AbstractJob {

  override def run(execDesc: JobExecDesc): Unit = {
    EchoHelloJob.doTest(execDesc, log => logger.debug(log))
  }
}
