package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.cli.CliSystem
import asura.ui.cli.task.TaskInfo
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class RunApi() extends ApiHandler {

  override def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq() =>
        CliSystem.sendToPool(extractTo(classOf[TaskInfo]))
        Future.successful(true)
      case _ => super.post(path)
    }
  }

}
