package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.Workspace
import asura.ui.ide.ops.WorkspaceOps.QueryWorkspace
import asura.ui.ide.query.{Paged, PagedResults}

trait WorkspaceOps {

  def insert(item: Workspace): Future[Long]

  def get(name: String): Future[Workspace]

  def search(params: QueryWorkspace): Future[PagedResults[Workspace]]

}

object WorkspaceOps {

  case class QueryWorkspace(username: String) extends Paged

}
