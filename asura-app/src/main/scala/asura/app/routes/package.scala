package asura.app

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import asura.app.routes.api.apiRoutes
import asura.app.routes.es.esRoutes
import asura.app.routes.job.jobRoutes
import asura.app.routes.linker.linkerRoutes
import asura.app.routes.ws.wsRoutes

package object routes extends HomeRoutes {

  lazy val allRoutes: Route =
    handleExceptions(RoutesExceptionHandler.handler) {
      homeRoutes ~
        apiRoutes ~
        handleRejections(RoutesRejectionHandler.handler) {
          authorizeAsync(ctx => Authorization.hasPermissions("approved token", ctx)) {
            jobRoutes ~
              wsRoutes ~
              esRoutes ~
              linkerRoutes
          }
        }
    }
}
