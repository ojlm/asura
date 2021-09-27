package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.ide.model.Project
import asura.ui.ide.ops.ProjectOps.QueryProject
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class ProjectApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project) => ide.project.get(workspace, project)
      case _ => super.get(path)
    }
  }

  override def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, "search") =>
        val body = extractTo(classOf[QueryProject])
        body.workspace = workspace
        ide.project.search(body)
      case _ => super.post(path)
    }
  }

  override def put(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq() =>
        val body = extractTo(classOf[Project])
        body.creator = currentUser
        ide.project.insert(body)
      case _ => super.get(path)
    }
  }

}
