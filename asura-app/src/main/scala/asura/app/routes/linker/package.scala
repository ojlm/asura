package asura.app.routes

import akka.http.scaladsl.server.Directives._

package object linker {
  lazy val linkerRoutes =
    pathPrefix("linker") {
      LinkerRoutes.linkerRoutes
    }
}
