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

  override def rename(id: String, item: TreeObject): Future[String] = {
    Future {
      val tree = getByIdSync(id).orNull
      if (tree == null) throw IdeErrors.TREE_DOC_MISS
      item.parse(true)
      val toUp = tree.copy(name = item.name, extension = item.extension)
      toUp.updatedAt = DateUtils.now()
      toUp.creator = item.creator
      deleteSync(id)
      saveSync(id, toUp)
    }
  }

  override def delete(id: String, recursive: Boolean): Future[String] = {
    Future {
      val item = getByIdSync(id)
      if (item.nonEmpty) {
        deleteItemSync(item.get, recursive)
      } else {
        throw IdeErrors.TREE_DOC_MISS
      }
    }
  }

  override def delete(item: TreeObject, recursive: Boolean): Future[String] = {
    Future(deleteItemSync(item, recursive))
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
        item.parse(false)
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

  override def rename(workspace: String, project: String, oldPath: Seq[String], newPath: Seq[String]): Future[String] = {
    Future {
      val oldTree = getByPathSync(workspace, project, oldPath)
      if (oldTree != null) {
        oldTree.name = newPath.last
        oldTree.parse(true)
        oldTree.updatedAt = DateUtils.now()
        if (oldPath.slice(0, oldPath.length - 1) == newPath.slice(0, newPath.length - 1)) { // same path
          deleteSync(oldTree.id)
          saveSync(oldTree.id, oldTree)
        } else {
          val newTree = getByPathSync(workspace, project, newPath)
          if (newTree == null) { // newPath do not have a file
            val newParentPath = newPath.slice(0, newPath.length - 1)
            val newParent = getByPathSync(workspace, project, newParentPath)
            if (newParent != null) {
              oldTree.parent = newParent.id
              deleteSync(oldTree.id)
              saveSync(oldTree.id, oldTree)
            } else {
              throw IdeErrors.TREE_DOC_MISS_MSG(newParentPath.mkString("/"))
            }
          } else {
            throw IdeErrors.TREE_NAME_EXISTS_MSG(newPath.mkString("/"))
          }
        }
      } else {
        throw IdeErrors.TREE_DOC_MISS_MSG(oldPath.mkString("/"))
      }
    }
  }

  override def delete(workspace: String, project: String, path: Seq[String], recursive: Boolean): Future[String] = {
    Future {
      val item = getByPathSync(workspace, project, path)
      if (item != null) {
        deleteItemSync(item, recursive)
      } else {
        throw IdeErrors.TREE_DOC_MISS_MSG(path.mkString("/"))
      }
    }
  }

  override def copy(workspace: String, project: String, source: Seq[String], destination: Seq[String]): Future[String] = {
    Future { // todo
      throw IdeErrors.TREE_DOC_MISS
    }
  }

  def deleteItemSync(item: TreeObject, recursive: Boolean): String = {
    if (item.`type` != TreeObject.TYPE_DIRECTORY) {
      deleteSync(item.id)
      // todo blob?
    } else {
      if (recursive) { // todo all child
        throw IdeErrors.TREE_DIRECTORY_DELETE
      } else {
        throw IdeErrors.TREE_DIRECTORY_DELETE
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
      var cur = 0
      val len = path.length
      path.foldLeft[TreeObject](null)((last, name) => {
        val item = if (last == null) {
          getByNameSync(workspace, project, null, name)
        } else {
          getByNameSync(workspace, project, last.id, name)
        }
        cur = cur + 1
        if (item == null && cur != len) {
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
    item.parse(false)
    if (!existsSync(item.workspace, item.project, item.parent, item.name)) {
      index(modelToDoc(item))
    } else {
      throw IdeErrors.TREE_NAME_EXISTS
    }
  }

}
