package asura.ui.cli.server.api

import scala.concurrent.Future

import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class WorkspaceApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(name) => ide.workspace.get(name)
      case _ => super.get(path)
    }
  }

}
