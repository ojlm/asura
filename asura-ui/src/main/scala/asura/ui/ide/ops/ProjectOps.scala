package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.Project
import asura.ui.ide.ops.ProjectOps.QueryProject
import asura.ui.ide.query.{Paged, PagedResults}

trait ProjectOps {

  def insert(project: Project): Future[String]

  def delete(id: String): Future[Boolean]

  def get(id: String): Future[Project]

  def update(  target: Project): Future[Project]

  def search(query: QueryProject): Future[PagedResults[Project]]

}

object ProjectOps {

  case class QueryProject(workspace: String) extends Paged

}
