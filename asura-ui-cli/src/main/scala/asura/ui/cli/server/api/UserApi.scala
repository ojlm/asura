package asura.ui.cli.server.api

import scala.concurrent.Future

import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class UserApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq("preference") => ide.user.getPreference(currentUser)
      case _ => super.get(path)
    }
  }

}
