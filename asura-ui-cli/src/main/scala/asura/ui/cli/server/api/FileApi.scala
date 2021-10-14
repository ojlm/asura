package asura.ui.cli.server.api

import java.util.Base64

import scala.concurrent.Future

import asura.common.util.StringUtils
import asura.ui.cli.server.api.FileApi.{CopyFileRequest, RenameFileRequest, WriteFileRequest}
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
      case Seq(workspace, project, "rename") =>
        val params = extractTo(classOf[RenameFileRequest])
        ide.tree.rename(workspace, project, params.oldPath, params.newPath)
      case Seq(workspace, project, "copy") =>
        val params = extractTo(classOf[CopyFileRequest])
        ide.tree.copy(workspace, project, params.source, params.destination)
      case Seq(workspace, project, "delete", tail@_*) =>
        ide.tree.delete(workspace, project, tail, true)
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
        val params = extractTo(classOf[WriteFileRequest])
        val item = TreeObject(workspace = workspace, project = project, name = tail.last)
        item.`type` = TreeObject.TYPE_FILE
        item.creator = currentUser
        item.size = params.data.length
        ide.tree.writeFile(tail, item, params.toBytes)
      case _ => super.get(path)
    }
  }

}

object FileApi {

  case class RenameFileRequest(var oldPath: Seq[String], var newPath: Seq[String])

  case class CopyFileRequest(var source: Seq[String], var destination: Seq[String])

  case class WriteFileRequest(var data: String) {
    def toBytes: Array[Byte] = {
      if (StringUtils.isNotEmpty(data)) Base64.getDecoder.decode(data) else Array.empty
    }
  }

}
