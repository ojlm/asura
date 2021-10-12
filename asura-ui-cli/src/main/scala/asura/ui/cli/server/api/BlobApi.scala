package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.ide.model.BlobObject
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class BlobApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, id) => ide.blob.getById(workspace, project, id)
      case _ => super.get(path)
    }
  }

  override def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(_, _, id) =>
        val body = extractTo(classOf[BlobObject])
        body.creator = currentUser
        ide.blob.update(id, body)
      case _ => super.get(path)
    }
  }

  override def put(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, tree) =>
        val body = extractTo(classOf[BlobObject])
        body.workspace = workspace
        body.project = project
        body.tree = tree
        body.creator = currentUser
        ide.blob.insert(body)
      case _ => super.get(path)
    }
  }

}
