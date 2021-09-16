package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.Activity
import asura.ui.ide.ops.ActivityOps.QueryActivity
import asura.ui.ide.query.{Paged, PagedResults}

trait ActivityOps {

  def insert(item: Activity): Future[Long] = insert(Seq(item))

  def insert(list: Seq[Activity]): Future[Long]

  def search(params: QueryActivity): Future[PagedResults[Activity]]

}

object ActivityOps {

  case class QueryActivity(creator: String) extends Paged

}
