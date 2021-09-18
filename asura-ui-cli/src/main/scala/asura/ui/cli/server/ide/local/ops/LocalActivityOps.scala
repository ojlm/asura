package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.common.util.StringUtils
import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene._
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.ide.model.Activity
import asura.ui.ide.ops.ActivityOps
import asura.ui.ide.ops.ActivityOps.QueryActivity
import asura.ui.ide.query.PagedResults

class LocalActivityOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[Activity](ide.config.PATH_ACTIVITY) with ActivityOps {

  val workspace = define.field[String]("workspace", FieldType.UN_TOKENIZED)
  val op = define.field[Int]("op", FieldType.NUMERIC)
  val project = define.field[String]("project", FieldType.UN_TOKENIZED)
  val target = define.field[String]("target", FieldType.UN_TOKENIZED)

  override val docToModel: SearchResult => Activity = doc => {
    val item = Activity(
      workspace = doc(workspace),
      op = doc(op),
      project = doc(project),
      target = doc(target),
    )
    fillCommonField(item, doc)
  }

  override val modelToDoc: Activity => DocumentBuilder = item => {
    val builder = doc().fields(
      workspace(item.workspace),
      op(item.op),
      project(item.project),
      target(item.target),
    )
    fillCommonField(builder, item)
  }

  override def insert(list: Seq[Activity]): Future[String] = {
    Future {
      index(list.map(modelToDoc): _*)
    }
  }

  def insertSync(item: Activity): String = {
    index(modelToDoc(item))
  }

  override def search(params: QueryActivity): Future[PagedResults[Activity]] = {
    Future(searchSync(params))
  }

  def searchSync(params: QueryActivity): PagedResults[Activity] = {
    val builder = query(docToModel)
    if (StringUtils.isEmpty(params.creator)) {
      builder.filter(term(creator(params.creator)))
    }
    val results = builder.offset(params.offset).limit(params.limit).search()
    PagedResults(results.total, results.entries)
  }

}
