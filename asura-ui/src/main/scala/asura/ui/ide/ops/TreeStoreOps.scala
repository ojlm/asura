package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.TreeObject
import asura.ui.ide.query.{Paged, PagedResults}

trait TreeStoreOps {

  def insert(item: TreeObject): Future[String]

  def getParent(workspace: String, project: String, id: String): Future[TreeObject]

  def getParent(treeItem: TreeObject): Future[TreeObject]

  def getChildren(workspace: String, project: String, id: String, paged: Paged): Future[PagedResults[TreeObject]]

  def exists(workspace: String, project: String, parent: String, name: String): Future[Boolean]

  def get(workspace: String, project: String, id: String): Future[TreeObject]

  def rename(id: String, item: TreeObject): Future[String]

  def delete(id: String, recursive: Boolean): Future[String]

  def delete(item: TreeObject, recursive: Boolean): Future[String]

  def updateBlob(id: String, blob: String, size: Long): Future[String]

  def get(workspace: String, project: String, path: Seq[String]): Future[TreeObject]

  def getChildren(workspace: String, project: String, path: Seq[String], paged: Paged): Future[PagedResults[TreeObject]]

  def createDirectory(path: Seq[String], item: TreeObject): Future[String]

  def writeFile(path: Seq[String], item: TreeObject, data: Array[Byte]): Future[String]

  def rename(workspace: String, project: String, oldPath: Seq[String], newPath: Seq[String]): Future[String]

  def delete(workspace: String, project: String, path: Seq[String], recursive: Boolean): Future[String]

  def copy(workspace: String, project: String, source: Seq[String], destination: Seq[String]): Future[String]

}

object TreeStoreOps {

  case class QueryTree(var workspace: String, var project: String, name: String) extends Paged

}
