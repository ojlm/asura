package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.ide.model.Task
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class TaskApi() extends ApiHandler {

  override def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case _ => super.post(path)
    }
  }

}

object TaskApi {


}
