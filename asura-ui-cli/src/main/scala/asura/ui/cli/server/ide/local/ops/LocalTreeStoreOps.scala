package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.common.util.{DateUtils, StringUtils}
import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene._
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.{BlobObject, TreeObject}
import asura.ui.ide.ops.TreeStoreOps
import asura.ui.ide.query.{Paged, PagedResults}

class LocalTreeStoreOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[TreeObject](ide.config.PATH_TREE) with TreeStoreOps {

  val workspace = define.field[String]("workspace", FieldType.UN_TOKENIZED)
  val project = define.field[String]("project", FieldType.UN_TOKENIZED)
  val name = define.field[String]("name", FieldType.UN_TOKENIZED, fullTextSearchable = true)
  val blob = define.field[String]("blob", FieldType.UN_TOKENIZED)
  val extension = define.field[String]("extension", FieldType.UN_TOKENIZED)
  val parent = define.field[String]("parent", FieldType.UN_TOKENIZED)
  val size = define.field[Long]("size", FieldType.NUMERIC)
  val `type` = define.field[Int]("type", FieldType.NUMERIC)

  override val docToModel: SearchResult => TreeObject = doc => {
    val item = TreeObject(
      workspace = doc(workspace),
      project = doc(project),
      name = doc(name),
      blob = doc(blob),
      extension = doc(extension),
      parent = doc(parent),
      size = doc(size),
      `type` = doc(`type`),
    )
    fillCommonField(item, doc)
  }

  override val modelToDoc: TreeObject => DocumentBuilder = item => {
    val builder = doc().fields(
      workspace(item.workspace),
      project(item.project),
      name(item.name),
      blob(item.blob),
      extension(item.extension),
      parent(item.parent),
      size(item.size),
      `type`(item.`type`),
    )
    fillCommonField(builder, item)
  }

  override def insert(item: TreeObject): Future[String] = {
    Future(insertSync(item))
  }

  override def getParent(workspace: String, project: String, id: String): Future[TreeObject] = {
    getParent(getByIdSync(workspace, project, id))
  }

  override def getParent(treeItem: TreeObject): Future[TreeObject] = {
    get(treeItem.workspace, treeItem.project, treeItem.parent)
  }

  override def getChildren(workspace: String, project: String, id: String, paged: Paged): Future[PagedResults[TreeObject]] = {
    Future(getChildrenSync(workspace, project, id, paged))
  }

  override def exists(workspace: String, project: String, parent: String, name: String): Future[Boolean] = {
    Future(existsSync(workspace, project, parent, name))
  }

  override def get(workspace: String, project: String, id: String): Future[TreeObject] = {
    Future(getByIdSync(workspace, project, id))
  }

  override def updateBlob(id: String, blob: String, size: Long): Future[String] = {
    Future(updateBlobSync(id, blob, size))
  }

  override def get(workspace: String, project: String, path: Seq[String]): Future[TreeObject] = {
    Future(getByPathSync(workspace, project, path))
  }

  override def getChildren(workspace: String, project: String, path: Seq[String], paged: Paged): Future[PagedResults[TreeObject]] = {
    Future {
      if (path == null || path.isEmpty) {
        getChildrenSync(workspace, project, null, paged)
      } else {
        val tree = getByPathSync(workspace, project, path)
        if (tree == null) {
          throw IdeErrors.TREE_DOC_MISS
        } else {
          getChildrenSync(workspace, project, tree.id, paged)
        }
      }
    }
  }

  override def createDirectory(path: Seq[String], item: TreeObject): Future[String] = {
    Future {
      if (path.length == 1) {
        insertSync(item)
      } else {
        val parent = getByPathSync(item.workspace, item.project, path.slice(0, path.length - 1))
        item.parent = parent.id
        insertSync(item)
      }
    }
  }

  override def writeFile(path: Seq[String], item: TreeObject, data: Array[Byte]): Future[String] = {
    Future {
      var parentId: String = null
      val treeItem = if (path.length == 1) {
        getByNameSync(item.workspace, item.project, null, item.name)
      } else {
        val parent = getByPathSync(item.workspace, item.project, path.slice(0, path.length - 1))
        if (parent == null) {
          throw IdeErrors.TREE_DOC_MISS
        } else {
          parentId = parent.id
          getByNameSync(item.workspace, item.project, parent.id, item.name)
        }
      }
      if (treeItem == null) { // new file
        val treeId = nextId()
        val blobId = ide.blob.nextId()
        item.blob = blobId
        item.parent = parentId
        item.parse()
        saveSync(treeId, item)
        val blob = BlobObject(workspace = item.workspace, project = item.project, tree = treeId, data = data, size = item.size)
        blob.creator = item.creator
        ide.blob.saveSync(blobId, blob)
        treeId
      } else { // update data
        val blob = BlobObject(workspace = item.workspace, project = item.project, tree = treeItem.id, data = data, size = item.size)
        blob.creator = item.creator
        ide.blob.update(treeItem.blob, blob)
        treeItem.id
      }
    }
  }

  def updateBlobSync(id: String, blob: String, size: Long): String = {
    val item = getByIdSync(id).orNull
    if (item == null) throw IdeErrors.TREE_DOC_MISS
    delete(this.id(id))
    val toUp = item.copy(blob = blob, size = size)
    toUp.updatedAt = DateUtils.now()
    index(id, modelToDoc(toUp))
  }

  def existsSync(workspace: String, project: String, parent: String, name: String): Boolean = {
    val results = query().filter(
      exact(this.name(name)),
      exact(this.parent(if (parent != null) parent else StringUtils.EMPTY)),
      exact(this.project(project)),
      exact(this.workspace(workspace)),
    ).limit(1).search()
    results.total > 0
  }

  def getByIdSync(workspace: String, project: String, id: String): TreeObject = {
    query(docToModel).filter(
      exact(this.id(id)),
      exact(this.project(project)),
      exact(this.workspace(workspace)),
    ).limit(1).search().entries.headOption.orNull
  }

  def getChildrenSync(workspace: String, project: String, id: String, paged: Paged): PagedResults[TreeObject] = {
    val q = query(docToModel).filter(
      exact(this.parent(if (StringUtils.isNotEmpty(id)) id else StringUtils.EMPTY)),
      exact(this.project(project)),
      exact(this.workspace(workspace))
    ).offset(paged.offset).limit(paged.limit)
    val results = q.search()
    PagedResults(results.total, results.entries)
  }

  def getByPathSync(workspace: String, project: String, path: Seq[String]): TreeObject = {
    if (path == null || path.isEmpty) {
      throw IdeErrors.TREE_PATH_EMPTY
    } else {
      path.foldLeft[TreeObject](null)((last, name) => {
        val item = if (last == null) {
          getByNameSync(workspace, project, null, name)
        } else {
          getByNameSync(workspace, project, last.id, name)
        }
        if (item == null) {
          throw IdeErrors.TREE_DOC_MISS
        } else {
          item
        }
      })
    }
  }

  def getByNameSync(workspace: String, project: String, parent: String, name: String): TreeObject = {
    query(docToModel).filter(
      exact(this.name(name)),
      exact(this.parent(if (parent != null) parent else StringUtils.EMPTY)),
      exact(this.project(project)),
      exact(this.workspace(workspace)),
    ).limit(1).search().entries.headOption.orNull
  }

  def insertSync(item: TreeObject): String = {
    item.parse()
    if (!existsSync(item.workspace, item.project, item.parent, item.name)) {
      index(modelToDoc(item))
    } else {
      throw IdeErrors.TREE_NAME_EXISTS
    }
  }

}
