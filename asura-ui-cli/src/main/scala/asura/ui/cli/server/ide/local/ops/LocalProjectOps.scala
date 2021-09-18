package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene.DocumentBuilder
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.ide.model.Project
import asura.ui.ide.ops.ProjectOps
import asura.ui.ide.ops.ProjectOps.QueryProject
import asura.ui.ide.query.PagedResults

class LocalProjectOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore(ide.config.PATH_PROJECT) with ProjectOps {

  override val docToModel: SearchResult => Nothing = null
  override val modelToDoc: Nothing => DocumentBuilder = null

  override def insert(project: Project): Future[String] = ???

  override def delete(id: String): Future[Boolean] = ???

  override def get(id: String): Future[Project] = ???

  override def update(target: Project): Future[Project] = ???

  override def search(query: QueryProject): Future[PagedResults[Project]] = ???

}
