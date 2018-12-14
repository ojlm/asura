package asura.core.job.impl

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import asura.core.CoreConfig
import asura.core.es.model.RestApiOnlineLog.GroupProject
import asura.core.es.model._
import asura.core.es.service._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.typesafe.scalalogging.Logger
import org.quartz.{Job, JobExecutionContext}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class SyncOnlineDomainAndRestApiJob extends Job {

  import SyncOnlineDomainAndRestApiJob._

  val logger = Logger(classOf[SyncOnlineDomainAndRestApiJob])

  override def execute(context: JobExecutionContext): Unit = {
    val detail = context.getJobDetail
    val dayCount = detail.getJobDataMap.getInt(KEY_DAY)
    val domainCount = detail.getJobDataMap.getInt(KEY_DOMAIN_COUNT)
    val apiCount = detail.getJobDataMap.getInt(KEY_API_COUNT)
    if (domainCount > 0) {
      val projectCoverageLogs = ArrayBuffer[ProjectApiCoverage]()
      val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(CoreConfig.onlineLogDatePattern))
      import asura.common.util.FutureUtils.RichFuture
      val domainLogs = DomainOnlineLogService.getOnlineDomain(domainCount, yesterday).await
      domainLogs.foreach(domainCountLog => {
        // get all apis of each domain
        val apiLogs = OnlineRequestLogService.getOnlineApi(domainCountLog.name, domainCountLog.count, apiCount).await
        if (apiLogs.nonEmpty) {
          // get all projects of each domain
          val projects = ProjectService.getProjectsByDomain(domainCountLog.name).await
          if (projects.nonEmpty) {
            // key: {method}{urlPath}
            val apiMap = mutable.HashMap[String, RestApiOnlineLog]()
            val apisShouldQuery = ArrayBuffer[Query]()
            apiLogs.foreach(apiLog => {
              // every log should have a copy of (group, project, covered)
              apiLog.belongs = projects.map(p => GroupProject(p.group, p.id))
              apiMap += (s"${apiLog.method}${apiLog.urlPath}" -> apiLog)
              apisShouldQuery += boolQuery().must(
                termQuery(FieldKeys.FIELD_OBJECT_REQUEST_METHOD, apiLog.method),
                termQuery(FieldKeys.FIELD_OBJECT_REQUEST_URLPATH, apiLog.urlPath)
              )
            })
            val domainApiSet = mutable.HashMap[String, Long]()
            projects.foreach(project => {
              // get all apis of each project
              val projectApiSet = CaseService.getApiSet(project, apisShouldQuery, apiLogs.size).await
              projectApiSet.foreach(projectApiItem => {
                domainApiSet += projectApiItem
                apiMap.get(projectApiItem._1).foreach(apiLog => {
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
                date = yesterday,
                coverage = Math.round((projectApiSet.size * 10000L).toDouble / apiLogs.size.toDouble).toInt
              )
            })
            domainCountLog.coverage = Math.round((domainApiSet.size * 10000L).toDouble / apiLogs.size.toDouble).toInt
          }
          RestApiOnlineLogService.index(apiLogs, domainCountLog.date).await
        }
      })
      if (domainLogs.nonEmpty) DomainOnlineLogService.index(domainLogs)
      if (projectCoverageLogs.nonEmpty) ProjectApiCoverageService.index(projectCoverageLogs)
    }
    if (dayCount > 0) {
      deleteOutdatedIndices(dayCount)
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
}
