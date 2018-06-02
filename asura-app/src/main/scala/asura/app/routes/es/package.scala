package asura.app.routes

import akka.http.scaladsl.server.Directives._
import asura.common.model.{ApiRes, ApiResError}
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}
import com.typesafe.scalalogging.Logger

package object es {

  val logger = Logger("EsRoutes")

  lazy val esRoutes =
    pathPrefix("es") {
      GroupRoutes.groupRoutes ~
        ProjectRoutes.projectRoutes ~
        ApiRoutes.apiRoutes ~
        CaseRoutes.caseRoutes ~
        EnvRoutes.envRoutes
    }

  def defaultEsResponseHandler(res: Either[RequestFailure, RequestSuccess[_]]): ApiRes = {
    res match {
      case Right(success) => ApiRes(data = success.result)
      case Left(failure) =>
        logger.warn(failure.error.reason)
        ApiResError(failure.error.reason)
    }
  }
}
