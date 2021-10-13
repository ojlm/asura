package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.common.util.{DateUtils, StringUtils}
import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.cli.store.lucene.{DocumentBuilder, exact}
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.BlobObject
import asura.ui.ide.ops.BlobStoreOps
import asura.ui.ide.query.{Paged, PagedResults}
import org.apache.lucene.util.BytesRef

class LocalBlobStoreOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[BlobObject](ide.config.PATH_BLOB) with BlobStoreOps {

  val workspace = define.field[String]("workspace", FieldType.UN_TOKENIZED)
  val project = define.field[String]("project", FieldType.UN_TOKENIZED)
  val tree = define.field[String]("tree", FieldType.UN_TOKENIZED)
  val data = define.field[BytesRef]("data", FieldType.UN_TOKENIZED, filterable = false, sortable = false)
  val size = define.field[Long]("size", FieldType.NUMERIC)

  override val docToModel: SearchResult => BlobObject = doc => {
    val item = docToModelWithoutData(doc)
    item.data = doc(data).bytes
    item
  }

  val docToModelWithoutData: SearchResult => BlobObject = doc => {
    val item = BlobObject(
      workspace = doc(workspace),
      project = doc(project),
      tree = doc(tree),
      data = null,
      size = doc(size),
    )
    fillCommonField(item, doc)
  }

  override val modelToDoc: BlobObject => DocumentBuilder = item => {
    val builder = doc().fields(
      workspace(item.workspace),
      project(item.project),
      tree(item.tree),
      data(new BytesRef(item.data)),
      size(item.size),
    )
    fillCommonField(builder, item)
  }

  override def insert(blob: BlobObject): Future[String] = {
    Future {
      blob.parse(false)
      val id = saveSync(blob)
      ide.tree.updateBlobSync(blob.tree, id, blob.data.length)
      id
    }
  }

  override def update(id: String, blob: BlobObject): Future[String] = {
    Future {
      blob.parse(true)
      val item = getByIdSync(id).orNull
      if (item == null) throw IdeErrors.BLOB_DOC_MISS
      val toUp = item.copy(data = blob.data, size = blob.size)
      toUp.updatedAt = DateUtils.now()
      toUp.creator = blob.creator
      deleteSync(id)
      saveSync(id, toUp)
      ide.tree.updateBlobSync(blob.tree, id, blob.size)
      id
    }
  }

  override def get(workspace: String, project: String, id: String): Future[BlobObject] = {
    Future(getSync(workspace, project, id))
  }

  override def getTreeBlobs(tree: String, paged: Paged): Future[PagedResults[BlobObject]] = {
    Future {
      val q = query(docToModelWithoutData).filter(this.tree(tree))
        .offset(paged.offset).limit(paged.limit)
      val results = q.search()
      PagedResults(results.total, results.entries)
    }
  }

  override def get(workspace: String, project: String, path: Seq[String]): Future[BlobObject] = {
    Future {
      val tree = ide.tree.getByPathSync(workspace, project, path)
      if (tree != null) {
        if (StringUtils.isEmpty(tree.blob)) {
          throw IdeErrors.BLOB_DOC_MISS
        } else {
          val item = getSync(workspace, project, tree.blob)
          if (item == null) throw IdeErrors.BLOB_DOC_MISS else item
        }
      } else {
        throw IdeErrors.TREE_DOC_MISS
      }
    }
  }

  def getSync(workspace: String, project: String, id: String): BlobObject = {
    query(docToModel).filter(
      exact(this.id(id)),
      exact(this.project(project)),
      exact(this.workspace(workspace)),
    ).limit(1).search().entries.headOption.orNull
  }

}
