package asura.core.job

import asura.common.util.StringUtils
import asura.core.job.impl.RunCaseJob
import org.quartz.Job

object JobCenter {

  var jobWorkDir: String = StringUtils.EMPTY
  var jobStdLogFileName: String = StringUtils.EMPTY
  /** (job name -> job meta) for web api usage */
  val supportedJobs = scala.collection.mutable.TreeMap[String, JobMeta]()
  /** (job classAlias -> Class) for Quartz build JobDetails */
  val supportedJobClasses = scala.collection.mutable.HashMap[String, Class[_ <: Job]]()
  /** (job classAlias -> JobBase) for JobData valid check and job test */
  val classAliasJobMap = scala.collection.mutable.HashMap[String, JobBase]()

  def init(jobWorkDir: String, jobStdLogFileName: String): Unit = {
    this.jobWorkDir = jobWorkDir
    this.jobStdLogFileName = jobStdLogFileName
    registerBuiltIn()
    registerFromFolder()
  }

  def register[T <: Job](job: JobBase, clazz: Class[T]): Unit = {
    val meta = job.meta
    require(!supportedJobs.contains(meta.name), s"Job's name ${meta.name} already exists, please change another name")
    supportedJobs += (meta.name -> meta)
    supportedJobClasses += (meta.classAlias -> clazz)
    classAliasJobMap += (meta.classAlias -> job)
  }

  def registerBuiltIn(): Unit = {
    JobCenter.register(RunCaseJob, classOf[RunCaseJob])
  }

  def registerFromFolder(): Unit = {
    // TODO
  }
}
