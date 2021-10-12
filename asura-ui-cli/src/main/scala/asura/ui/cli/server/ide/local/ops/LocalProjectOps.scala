package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.common.util.StringUtils
import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene._
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.Project
import asura.ui.ide.ops.ProjectOps
import asura.ui.ide.ops.ProjectOps.QueryProject
import asura.ui.ide.query.PagedResults

class LocalProjectOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[Project](ide.config.PATH_PROJECT) with ProjectOps {

  val workspace = define.field[String]("workspace", FieldType.UN_TOKENIZED)
  val name = define.field[String]("name", FieldType.UN_TOKENIZED, fullTextSearchable = true)
  val alias = define.field[String]("alias")
  val avatar = define.field[String]("avatar", FieldType.UN_TOKENIZED, sortable = false)
  val description = define.field[String]("description", fullTextSearchable = true, sortable = false)

  override val docToModel: SearchResult => Project = doc => {
    val item = Project(
      workspace = doc(workspace),
      name = doc(name),
      alias = doc(alias),
      avatar = doc(avatar),
      description = doc(description),
    )
    fillCommonField(item, doc)
  }

  override val modelToDoc: Project => DocumentBuilder = item => {
    val builder = doc().fields(
      workspace(item.workspace),
      name(item.name),
      alias(item.alias),
      description(item.description),
      avatar(item.avatar),
    )
    fillCommonField(builder, item)
  }

  override def insert(item: Project): Future[String] = {
    Future {
      if (StringUtils.isEmpty(item.workspace)) {
        throw IdeErrors.WORKSPACE_NAME_EMPTY
      } else if (!LocalIde.isNameLegal(item.name)) {
        throw IdeErrors.PROJECT_NAME_ILLEGAL
      } else {
        val results = query().filter(
          exact(this.name(item.name)),
          exact(this.workspace(item.workspace)),
        ).limit(1).search()
        if (results.total == 0) {
          index(modelToDoc(item))
        } else {
          throw IdeErrors.PROJECT_NAME_EXISTS
        }
      }
    }
  }

  override def get(workspace: String, name: String): Future[Project] = {
    Future {
      query(docToModel).filter(
        exact(this.name(name)),
        exact(this.workspace(workspace)),
      ).limit(1).search().entries.headOption.orNull
    }
  }

  override def update(target: Project): Future[Project] = ???

  override def search(params: QueryProject): Future[PagedResults[Project]] = {
    Future {
      var q = query(docToModel).offset(params.offset).limit(params.limit)
      if (StringUtils.isNotEmpty(params.workspace)) {
        q = q.filter(exact(this.workspace(params.workspace)))
      }
      val results = q.search()
      PagedResults(results.total, results.entries)
    }
  }

}
