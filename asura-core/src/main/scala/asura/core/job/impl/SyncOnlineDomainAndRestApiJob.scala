package asura.core.job.impl

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import asura.common.util.LogUtils
import asura.core.CoreConfig.EsOnlineLogConfig
import asura.core.es.EsClient
import asura.core.es.model.RestApiOnlineLog.GroupProject
import asura.core.es.model._
import asura.core.es.service._
import com.sksamuel.elastic4s.searches.queries.Query
import com.typesafe.scalalogging.Logger
import org.quartz.{Job, JobExecutionContext}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class SyncOnlineDomainAndRestApiJob extends Job {

  import SyncOnlineDomainAndRestApiJob._

  val logger = Logger(classOf[SyncOnlineDomainAndRestApiJob])

  override def execute(context: JobExecutionContext): Unit = {
    try {
      val detail = context.getJobDetail
      val dayCount = detail.getJobDataMap.getInt(KEY_DAY)
      val domainCount = detail.getJobDataMap.getInt(KEY_DOMAIN_COUNT)
      val jobApiCount = detail.getJobDataMap.getInt(KEY_API_COUNT)
      val esOnlineConfigs = EsClient.esOnlineLogClients
      if (domainCount > 0 && esOnlineConfigs.nonEmpty) {
        esOnlineConfigs.foreach(config => syncSource(domainCount, jobApiCount, config))
      }
      if (dayCount > 0) {
        deleteOutdatedIndices(dayCount)
      }
    } catch {
      case t: Throwable => logger.error(LogUtils.stackTraceToString(t))
    }
  }

  private def syncSource(domainCount: Int, jobApiCount: Int, esConfig: EsOnlineLogConfig): Unit = {
    val projectCoverageLogs = ArrayBuffer[ProjectApiCoverage]()
    val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(esConfig.datePattern))
    import asura.common.util.FutureUtils.RichFuture
    val domainLogs = OnlineRequestLogService.getOnlineDomain(domainCount, yesterday, esConfig).await
    logger.debug(s"online domain count: ${domainLogs.size}")
    domainLogs.foreach(domainCountLog => {
      try {
        // get all apis of each domain
        val apiLogs = OnlineRequestLogService.getOnlineApi(domainCountLog.name, domainCountLog.count, jobApiCount, esConfig).await
        logger.debug(s"online api count of ${domainCountLog.name}: ${apiLogs.size}")
        if (apiLogs.nonEmpty) {
          // get all projects of each domain
          val projects = getProjectsOfDomain(domainCountLog.name)
          if (projects.nonEmpty) {
            // key: {method}{urlPath}
            val apiMap = mutable.HashMap[String, RestApiOnlineLog]()
            // val apisShouldQuery = ArrayBuffer[Query]()
            apiLogs.foreach(apiLog => {
              // every log should have a copy of (group, project, covered)
              apiLog.belongs = projects.map(p => GroupProject(p.group, p.id))
              apiMap += (s"${apiLog.method}${apiLog.urlPath}" -> apiLog)
              // apisShouldQuery += boolQuery().must(
              //   termQuery(FieldKeys.FIELD_OBJECT_REQUEST_METHOD, apiLog.method),
              //   termQuery(FieldKeys.FIELD_OBJECT_REQUEST_URLPATH, apiLog.urlPath)
              // )
            })
            val onlineApiCount = apiMap.size
            val domainApiSet = mutable.HashMap[String, Long]()
            projects.foreach(project => {
              // get all apis of each project
              var projectApiOnlineCount = 0
              val projectApiMaxSize = if (apiMap.size > 2000) apiMap.size * 2 else 2000
              val projectApiSet = getProjectApiSet(project, Nil, projectApiMaxSize)
              projectApiSet.foreach(projectApiItem => {
                // only if the api is also online
                apiMap.get(projectApiItem._1).foreach(apiLog => {
                  domainApiSet += projectApiItem
                  projectApiOnlineCount = projectApiOnlineCount + 1
                  apiLog.belongs
                    .filter(belong => belong.group == project.group && belong.project == project.id)
                    .foreach(belong => {
                      belong.covered = true
                      belong.count = projectApiItem._2
                    })
                })
              })
              projectCoverageLogs += ProjectApiCoverage(
                group = project.group,
                project = project.id,
                domain = domainCountLog.name,
                tag = esConfig.tag,
                date = yesterday,
                coverage = {
                  if (onlineApiCount > 0) {
                    Math.round((projectApiOnlineCount * 10000L).toDouble / onlineApiCount.toDouble).toInt
                  } else {
                    0
                  }
                }
              )
            })
            domainCountLog.coverage = {
              if (onlineApiCount > 0) {
                Math.round((domainApiSet.size * 10000L).toDouble / onlineApiCount.toDouble).toInt
              } else {
                0
              }
            }
          }
          RestApiOnlineLogService.index(apiLogs, domainCountLog.date).await
        }
      } catch {
        case t: Throwable => logger.error(s"${domainCountLog.name} ${LogUtils.stackTraceToString(t)}")
      }
    })
    if (domainLogs.nonEmpty) DomainOnlineLogService.index(domainLogs).await
    if (projectCoverageLogs.nonEmpty) ProjectApiCoverageService.index(projectCoverageLogs.toSeq).await
  }

  private def getProjectApiSet(project: Project, apisQuery: Seq[Query], aggSize: Int): Map[String, Long] = {
    import asura.common.util.FutureUtils.RichFuture
    try {
      HttpCaseRequestService.getApiSet(project, Nil, aggSize).await.toMap
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        Map.empty
    }
  }

  private def getProjectsOfDomain(domain: String): Seq[Project] = {
    import asura.common.util.FutureUtils.RichFuture
    try {
      ProjectService.getProjectsByDomain(domain).await
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        Nil
    }
  }

  def deleteOutdatedIndices(dayCount: Int): Unit = {
    import asura.common.util.FutureUtils.RichFuture
    val response = RestApiOnlineLogService.getIndices().await
    if (response.isSuccess) {
      val indices = response.result.slice(dayCount, response.result.size).map(_.index)
      if (indices.nonEmpty) {
        logger.info(s"delete indices: ${indices.mkString(",")}")
        IndexService.delIndex(indices).await
      }
    } else {
      logger.error(response.error.reason)
    }
  }
}

object SyncOnlineDomainAndRestApiJob {

  val NAME = "SyncOnlineDomainAndRestApiJob"
  val KEY_CRON = "cron"
  val KEY_DAY = "day"
  val KEY_DOMAIN_COUNT = "domainCount"
  val KEY_API_COUNT = "apiCount"

  val DEFAULT_DAY = 20
  val DEFAULT_DOMAIN_COUNT = 1000
  val DEFAULT_API_COUNT = 2000
}
