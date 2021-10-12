package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.common.util.{DateUtils, StringUtils}
import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.cli.store.lucene.{DocumentBuilder, exact}
import asura.ui.ide.IdeErrors
import asura.ui.ide.model.TreeObject
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
    Future {
      item.parse()
      if (!existsSync(item.workspace, item.project, item.parent, item.name)) {
        index(modelToDoc(item))
      } else {
        throw IdeErrors.TREE_NAME_EXISTS
      }
    }
  }

  override def getParent(workspace: String, project: String, id: String): Future[TreeObject] = {
    getParent(getByIdSync(workspace, project, id))
  }

  override def getParent(treeItem: TreeObject): Future[TreeObject] = {
    getByName(treeItem.workspace, treeItem.project, treeItem.parent)
  }

  override def getChildren(workspace: String, project: String, id: String, paged: Paged): Future[PagedResults[TreeObject]] = {
    Future {
      val q = query(docToModel).filter(
        exact(if (StringUtils.isNotEmpty(id)) this.parent(id) else this.parent(StringUtils.EMPTY)),
        exact(this.project(project)),
        exact(this.workspace(workspace))
      ).offset(paged.offset).limit(paged.limit)
      val results = q.search()
      PagedResults(results.total, results.entries)
    }
  }

  override def exists(workspace: String, project: String, parent: String, name: String): Future[Boolean] = {
    Future(existsSync(workspace, project, parent, name))
  }

  override def getByName(workspace: String, project: String, name: String): Future[TreeObject] = {
    Future(getByNameSync(workspace, project, name))
  }

  override def getById(workspace: String, project: String, id: String): Future[TreeObject] = {
    Future(getByIdSync(workspace, project, id))
  }

  override def updateBlob(id: String, blob: String, size: Long): Future[String] = {
    Future {
      val item = getByIdSync(id).orNull
      if (item == null) throw IdeErrors.TREE_DOC_MISS
      delete(this.id(id))
      val toUp = item.copy(blob = blob, size = size)
      toUp.updatedAt = DateUtils.now()
      index(id, modelToDoc(toUp))
    }
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

  def getByNameSync(workspace: String, project: String, name: String): TreeObject = {
    query(docToModel).filter(
      exact(this.name(name)),
      exact(this.project(project)),
      exact(this.workspace(workspace)),
    ).limit(1).search().entries.headOption.orNull
  }

}
