package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene._
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.ide.model.Task
import asura.ui.ide.ops.TaskOps
import org.apache.lucene.util.BytesRef

class LocalTaskOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[Task](ide.config.PATH_TASK) with TaskOps {

  val workspace = define.field[String]("workspace", FieldType.UN_TOKENIZED)
  val project = define.field[String]("project", FieldType.UN_TOKENIZED)
  val name = define.field[String]("name", fullTextSearchable = true)
  val description = define.field[String]("description", fullTextSearchable = true, sortable = false)
  val `type` = define.field[Int]("type", FieldType.NUMERIC)
  val driver = define.field[String]("driver", FieldType.UN_TOKENIZED)
  val data = define.field[BytesRef]("data", FieldType.UN_TOKENIZED, filterable = false, sortable = false)

  override val docToModel: SearchResult => Task = doc => {
    val item = Task(
      workspace = doc(workspace),
      project = doc(project),
      name = doc(name),
      description = doc(description),
      `type` = doc(`type`),
      driver = doc(driver),
    )
    fillCommonField(item, doc)
  }

  override val modelToDoc: Task => DocumentBuilder = item => {
    val builder = doc().fields(
      workspace(item.workspace),
      project(item.project),
      name(item.name),
      description(item.description),
      `type`(item.`type`),
      driver(item.driver),
    )
    fillCommonField(builder, item)
  }

  override def insert(item: Task): Future[String] = ???

}
