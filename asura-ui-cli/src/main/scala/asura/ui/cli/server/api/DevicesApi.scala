package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.cli.CliSystem
import asura.ui.cli.actor.AndroidDeviceActor.ExecuteStepMessage
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class DevicesApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq() => CliSystem.getDevices()
      case Seq(serial, "step") =>
        CliSystem.executeStep(ExecuteStepMessage(serial, extractToString(req)))
      case _ => super.get(path)
    }
  }

}
