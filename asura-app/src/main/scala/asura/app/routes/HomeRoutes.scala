package asura.app.routes

import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqMarshaller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.pathEndOrSingleSlash
import akka.http.scaladsl.server.directives.RouteDirectives.complete

trait HomeRoutes {

  lazy val homeRoutes: Route =
    pathEndOrSingleSlash {
      complete(
        <html>
          <body>
            <h1 style="color:brown;">Asura</h1>
          </body>
        </html>
      )
    }
}
