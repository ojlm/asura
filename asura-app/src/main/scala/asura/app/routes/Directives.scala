package asura.app.routes

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._

object Directives {

  def asuraUser(): Directive1[(String)] = {
    provide("approved")
  }
}


