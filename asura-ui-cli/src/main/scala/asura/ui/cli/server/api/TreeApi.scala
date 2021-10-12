package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.ide.model.TreeObject
import asura.ui.ide.ops.BlobStoreOps.QueryBlob
import asura.ui.ide.ops.TreeStoreOps.QueryTree
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class TreeApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, id) => ide.tree.getById(workspace, project, id)
      case _ => super.get(path)
    }
  }

  override def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, "search") =>
        val params = extractTo(classOf[QueryTree])
        ide.tree.getChildren(workspace, project, null, params)
      case Seq(workspace, project, parent, "search") =>
        val params = extractTo(classOf[QueryTree])
        ide.tree.getChildren(workspace, project, parent, params)
      case Seq(_, _, tree, "blobs") =>
        val params = extractTo(classOf[QueryBlob])
        ide.blob.getTreeBlobs(tree, params)
      case _ => super.post(path)
    }
  }

  override def put(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project) =>
        val body = extractTo(classOf[TreeObject])
        body.workspace = workspace
        body.project = project
        body.creator = currentUser
        ide.tree.insert(body)
      case _ => super.get(path)
    }
  }

}
