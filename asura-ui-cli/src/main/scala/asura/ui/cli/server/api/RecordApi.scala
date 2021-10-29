package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.cli.CliSystem
import asura.ui.cli.server.api.RecordApi.ForkResponse
import asura.ui.ide.model.TaskRecord.RecordData
import asura.ui.ide.model.{Address, Task, TaskRecord}
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class RecordApi() extends ApiHandler {

  override def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, "fork") =>
        val item = extractTo(classOf[TaskRecord])
        item.workspace = workspace
        item.project = project
        item.`type` = Task.TYPE_DEBUG
        item.data = RecordData(addr = Nil)
        item.parse()
        ide.record.insert(item).map(id => ForkResponse(id, item.data.addr))(CliSystem.ec)
      case _ => super.post(path)
    }
  }

}

object RecordApi {

  case class ForkResponse(id: String, addr: Seq[Address])

}
