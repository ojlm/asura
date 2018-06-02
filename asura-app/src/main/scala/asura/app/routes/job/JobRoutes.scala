package asura.app.routes.job

import akka.http.scaladsl.server.Directives._
import asura.app.routes.Directives.asuraUser
import asura.common.model.{ApiRes, ApiResError, ApiResInvalid}
import asura.core.cs.model.QueryJobReport
import asura.core.es.EsResponse
import asura.core.es.service.{JobReportService, JobService}
import asura.core.job._
import asura.core.job.actor._
import asura.core.util.JacksonSupport._

import scala.util.{Failure, Success}

object JobRoutes {

  val jobRoutes = {
    pathPrefix("job") {
      path("type") {
        val jobTypes = JobCenter.supportedJobs.values.map(value => Map(
          "label" -> value.name,
          "value" -> value.classAlias,
          "desc" -> value.desc))
        complete(ApiRes(data = Map(
          "jobTypes" -> jobTypes, "schedulers" -> SchedulerManager.schedulers.keys()
        )))
      } ~
        path("new") {
          asuraUser() { username =>
            entity(as[NewJob]) { req =>
              val jobMeta = req.jobMeta
              val jobData = req.jobData
              val triggerMeta = req.triggerMeta
              JobUtils.validateJobAndTrigger(jobMeta, triggerMeta, jobData) match {
                case (true, _) =>
                  onComplete(SchedulerManager.scheduleJob(jobMeta, triggerMeta, jobData, username)) {
                    case Success(msg) => complete(ApiRes(msg = msg))
                    case Failure(t) => complete(ApiResError(t.getMessage))
                  }
                case (false, errMsg) => complete(ApiResInvalid(errMsg))
              }
            }
          }
        } ~
        path("update") {
          entity(as[UpdateJob]) { req =>
            onComplete(SchedulerManager.updateJob(req)) {
              case Success(msg) => complete(ApiRes(msg = msg))
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("pause") {
          entity(as[PauseJob]) { job =>
            val (result, msg) = SchedulerManager.pauseJob(job)
            if (result) {
              complete(ApiRes())
            } else {
              complete(ApiResError(msg))
            }
          }
        } ~
        path("resume") {
          entity(as[ResumeJob]) { job =>
            val (result, msg) = SchedulerManager.resumeJob(job)
            if (result) {
              complete(ApiRes())
            } else {
              complete(ApiResError(msg))
            }
          }
        } ~
        path("delete") {
          entity(as[DeleteJob]) { job =>
            val (result, msg) = SchedulerManager.deleteJob(job)
            if (result) {
              complete(ApiRes())
            } else {
              complete(ApiResError(msg))
            }
          }
        } ~
        path("log") {
          entity(as[QueryJobReport]) { report =>
            onComplete(JobReportService.query(report)) {
              case Success(res) => complete {
                res match {
                  case Right(success) => ApiRes(data = EsResponse.toApiData(success.result))
                  case Left(failure) => ApiResError(failure.error.reason)
                }
              }
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        path("detail") {
          parameter('id) { id =>
            onComplete(JobService.getById(id)) {
              case Success(res) => complete {
                res match {
                  case Right(success) => ApiRes(data = EsResponse.toSingleApiData(success.result))
                  case Left(failure) => ApiResError(failure.error.reason)
                }
              }
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        } ~
        pathPrefix("report") {
          path(Segment) { id =>
            onComplete(JobReportService.getById(id)) {
              case Success(res) => complete {
                res match {
                  case Right(success) => ApiRes(data = EsResponse.toSingleApiData(success.result))
                  case Left(failure) => ApiResError(failure.error.reason)
                }
              }
              case Failure(t) => complete(ApiResError(t.getMessage))
            }
          }
        }
    }
  }
}
