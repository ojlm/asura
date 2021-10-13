package asura.ui.cli.server.api

import scala.concurrent.Future

import asura.ui.cli.server.api.FileApi.WriteFileData
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.TreeObject
import asura.ui.ide.ops.TreeStoreOps.QueryTree
import karate.io.netty.handler.codec.http.{FullHttpRequest, QueryStringDecoder}

class FileApi() extends ApiHandler {

  override def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, "tree", tail@_*) => ide.tree.get(workspace, project, tail)
      case Seq(workspace, project, "blob", tail@_*) => ide.blob.get(workspace, project, tail)
      case _ => super.get(path)
    }
  }

  override def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, "children", tail@_*) =>
        val params = extractTo(classOf[QueryTree])
        ide.tree.getChildren(workspace, project, tail, params)
      case _ => super.post(path)
    }
  }

  override def put(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    path match {
      case Seq(workspace, project, "directory", tail@_*) =>
        val item = TreeObject(workspace = workspace, project = project, name = tail.last)
        item.`type` = TreeObject.TYPE_DIRECTORY
        item.creator = currentUser
        ide.tree.createDirectory(tail, item)
      case Seq(workspace, project, "file", tail@_*) =>
        val params = extractTo(classOf[WriteFileData])
        val item = TreeObject(workspace = workspace, project = project, name = tail.last)
        item.`type` = TreeObject.TYPE_FILE
        item.creator = currentUser
        if (params.data == null || params.data.isEmpty) throw IdeErrors.BLOB_DATA_EMPTY
        item.size = params.data.length
        ide.tree.writeFile(tail, item, params.data)
      case _ => super.get(path)
    }
  }

}

object FileApi {

  case class WriteFileData(data: Array[Byte])

}
