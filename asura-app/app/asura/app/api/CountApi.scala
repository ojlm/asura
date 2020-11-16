package asura.app.api

import asura.core.es.model._
import asura.core.es.service.CountService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class CountApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def all() = Action.async { implicit req =>
    val countRes = for {
      http <- CountService.countIndex(HttpStepRequest.Index)
      dubbo <- CountService.countIndex(DubboRequest.Index)
      sql <- CountService.countIndex(SqlRequest.Index)
      scenario <- CountService.countIndex(Scenario.Index)
      job <- CountService.countIndex(Job.Index)
      webHttp <- CountService.countActivity(Activity.TYPE_TEST_CASE)
      webDubbo <- CountService.countActivity(Activity.TYPE_TEST_DUBBO)
      webSql <- CountService.countActivity(Activity.TYPE_TEST_SQL)
      webScenario <- CountService.countActivity(Activity.TYPE_TEST_SCENARIO)
      webJob <- CountService.countActivity(Activity.TYPE_TEST_JOB)
      ciJob <- CountService.countJob(JobReport.TYPE_CI)
      quartzJob <- CountService.countJob(JobReport.TYPE_QUARTZ)
    } yield (
      http, dubbo, sql, scenario, job,
      webHttp, webDubbo, webSql, webScenario, webJob,
      ciJob, quartzJob,
    )
    countRes.map(tuple => {
      Map(
        "http" -> tuple._1,
        "dubbo" -> tuple._2,
        "sql" -> tuple._3,
        "scenario" -> tuple._4,
        "job" -> tuple._5,
        "webHttp" -> tuple._6,
        "webDubbo" -> tuple._7,
        "webSql" -> tuple._8,
        "webScenario" -> tuple._9,
        "webJob" -> tuple._10,
        "ciJob" -> tuple._11,
        "quartzJob" -> tuple._12,
      )
    }).flatMap(count => {
      val histogramRes = for {
        httpHistogram <- CountService.dateHistogram(HttpStepRequest.Index)
        dubboHistogram <- CountService.dateHistogram(DubboRequest.Index)
        sqlHistogram <- CountService.dateHistogram(SqlRequest.Index)
        scenarioHistogram <- CountService.dateHistogram(Scenario.Index)
        jobHistogram <- CountService.dateHistogram(Job.Index)
        webHttpHistogram <- CountService.activityDateHistogram(Activity.TYPE_TEST_CASE)
        webDubboHistogram <- CountService.activityDateHistogram(Activity.TYPE_TEST_DUBBO)
        webSqlHistogram <- CountService.activityDateHistogram(Activity.TYPE_TEST_SQL)
        webScenarioHistogram <- CountService.activityDateHistogram(Activity.TYPE_TEST_SCENARIO)
        webJobHistogram <- CountService.activityDateHistogram(Activity.TYPE_TEST_JOB)
        ciJobHistogram <- CountService.jobDateHistogram(JobReport.TYPE_CI)
        quartzJobHistogram <- CountService.jobDateHistogram(JobReport.TYPE_QUARTZ)
      } yield (
        httpHistogram, dubboHistogram, sqlHistogram, scenarioHistogram, jobHistogram,
        webHttpHistogram, webDubboHistogram, webSqlHistogram, webScenarioHistogram, webJobHistogram,
        ciJobHistogram, quartzJobHistogram
      )
      histogramRes.map(tuple => {
        Map(
          "http" -> tuple._1,
          "dubbo" -> tuple._2,
          "sql" -> tuple._3,
          "scenario" -> tuple._4,
          "job" -> tuple._5,
          "webHttp" -> tuple._6,
          "webDubbo" -> tuple._7,
          "webSql" -> tuple._8,
          "webScenario" -> tuple._9,
          "webJob" -> tuple._10,
          "ciJob" -> tuple._11,
          "quartzJob" -> tuple._12,
        )
      }).map(histogram => {
        Map("count" -> count, "histogram" -> histogram)
      })
    }).toOkResult
  }
}
