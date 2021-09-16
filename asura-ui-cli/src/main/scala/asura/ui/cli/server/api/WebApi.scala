package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.cli.CliSystem
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class WebApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(id) => CliSystem.getTask(id)
      case _ => super.get(path)
    }
  }

}
