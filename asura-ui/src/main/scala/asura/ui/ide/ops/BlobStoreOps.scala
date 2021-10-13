package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.BlobObject
import asura.ui.ide.query.{Paged, PagedResults}

trait BlobStoreOps {

  def insert(blob: BlobObject): Future[String]

  def update(id: String, blob: BlobObject): Future[String]

  def get(workspace: String, project: String, id: String): Future[BlobObject]

  def getTreeBlobs(tree: String, paged: Paged): Future[PagedResults[BlobObject]]

  def get(workspace: String, project: String, path: Seq[String]): Future[BlobObject]

}

object BlobStoreOps {

  case class QueryBlob() extends Paged

}
