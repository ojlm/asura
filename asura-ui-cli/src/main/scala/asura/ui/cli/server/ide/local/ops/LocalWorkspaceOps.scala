package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.cli.store.lucene.{DocumentBuilder, term}
import asura.ui.ide.model.Workspace
import asura.ui.ide.ops.WorkspaceOps
import asura.ui.ide.ops.WorkspaceOps.QueryWorkspace
import asura.ui.ide.query.PagedResults

class LocalWorkspaceOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[Workspace](ide.config.PATH_WORKSPACE) with WorkspaceOps {

  val name = define.field[String]("name", FieldType.UN_TOKENIZED, fullTextSearchable = true)
  val alias = define.field[String]("alias")
  val avatar = define.field[String]("avatar", FieldType.UN_TOKENIZED, sortable = false)
  val description = define.field[String]("description", fullTextSearchable = true, sortable = false)

  override val docToModel: SearchResult => Workspace = doc => {
    val item = Workspace(
      name = doc(name),
      alias = doc(alias),
      avatar = doc(avatar),
      description = doc(description),
    )
    fillCommonField(item, doc)
  }

  override val modelToDoc: Workspace => DocumentBuilder = item => {
    val builder = doc().fields(
      name(item.name),
      alias(item.alias),
      description(item.description),
      avatar(item.avatar),
    )
    fillCommonField(builder, item)
  }

  override def members(username: String, params: QueryWorkspace): Future[PagedResults[Workspace]] = {
    params.creator = username
    search(params)
  }

  override def insert(item: Workspace): Future[Long] = {
    Future {
      index(modelToDoc(item))
    }
  }

  override def get(name: String): Future[Workspace] = {
    Future {
      query(docToModel).filter(term(this.name(name))).limit(1).search().entries.headOption.orNull
    }
  }

  override def search(params: QueryWorkspace): Future[PagedResults[Workspace]] = {
    Future {
      val results = query(docToModel)
        .filter(term(this.creator(params.creator)))
        .offset(params.offset).limit(params.limit)
        .search()
      PagedResults(results.total, results.entries)
    }
  }

}
