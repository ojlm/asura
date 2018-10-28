package asura.app.api

import akka.actor.ActorSystem
import asura.core.ErrorMessages
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.service.JobReportDataService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext

@Singleton
class SystemApi @Inject()(
                           implicit val system: ActorSystem,
                           val exec: ExecutionContext,
                           val configuration: Configuration,
                           val controllerComponents: SecurityComponents,
                         ) extends BaseApi {

  val administrators = configuration.getOptional[Seq[String]]("asura.admin").getOrElse(Nil).toSet
  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getJobReportIndices() = Action.async { implicit req =>
    JobReportDataService.getIndices().map(res => {
      if (res.isSuccess) {
        toActionResultFromAny(res.result)
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    })
  }
}
