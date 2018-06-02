package asura.app.jobs

import asura.AppConfig
import asura.core.job.JobCenter

object JobManager {

  def init(): Unit = {
    JobCenter.init(AppConfig.jobWorkDir, AppConfig.jobStdLogFileName)
    JobCenter.register(EchoHelloJob, classOf[EchoHelloJob])
  }
}
