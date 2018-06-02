package asura.app.routes

import akka.http.scaladsl.server.Directives._
import asura.app.routes.ws.JobRoutes.jobWsRoutes

package object ws {

  lazy val wsRoutes =
    get {
      pathPrefix("ws") {
        jobWsRoutes
      }
    }
}
