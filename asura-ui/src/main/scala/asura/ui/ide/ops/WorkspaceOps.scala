package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.Workspace
import asura.ui.ide.ops.WorkspaceOps.QueryWorkspace
import asura.ui.ide.query.{Paged, PagedResults}

trait WorkspaceOps {

  def members(username: String, params: QueryWorkspace): Future[PagedResults[Workspace]]

  def insert(item: Workspace): Future[String]

  def get(name: String): Future[Workspace]

  def search(params: QueryWorkspace): Future[PagedResults[Workspace]]

}

object WorkspaceOps {

  case class QueryWorkspace(var creator: String) extends Paged

}
