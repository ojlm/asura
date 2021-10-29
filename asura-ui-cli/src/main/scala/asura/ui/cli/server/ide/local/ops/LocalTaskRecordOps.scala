package asura.ui.cli.server.ide.local.ops

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.server.ide.local.{LocalIde, LocalStore}
import asura.ui.cli.store.lucene._
import asura.ui.cli.store.lucene.field.FieldType
import asura.ui.cli.store.lucene.query.SearchResult
import asura.ui.ide.model.TaskRecord
import asura.ui.ide.ops.TaskRecordOps

class LocalTaskRecordOps(val ide: LocalIde)(implicit ec: ExecutionContext)
  extends LocalStore[TaskRecord](ide.config.PATH_RECORD) with TaskRecordOps {

  val workspace = define.field[String]("workspace", FieldType.UN_TOKENIZED)
  val project = define.field[String]("project", FieldType.UN_TOKENIZED)
  val task = define.field[String]("task", FieldType.UN_TOKENIZED)
  val `type` = define.field[Int]("type", FieldType.NUMERIC)
  val driver = define.field[String]("driver", FieldType.UN_TOKENIZED)
  val startAt = define.field[Long]("startAt", FieldType.NUMERIC)
  val endAt = define.field[Long]("endAt", FieldType.NUMERIC)
  val elapse = define.field[Long]("elapse", FieldType.NUMERIC)

  override val docToModel: SearchResult => TaskRecord = doc => {
    val item = TaskRecord(
      workspace = doc(workspace),
      project = doc(project),
      task = doc(task),
      `type` = doc(`type`),
      driver = doc(driver),
      startAt = doc(startAt),
      endAt = doc(endAt),
      elapse = doc(elapse),
    )
    fillCommonField(item, doc)
  }

  override val modelToDoc: TaskRecord => DocumentBuilder = item => {
    val builder = doc().fields(
      workspace(item.workspace),
      project(item.project),
      task(item.task),
      `type`(item.`type`),
      driver(item.driver),
      startAt(item.startAt),
      endAt(item.endAt),
      elapse(item.elapse),
    )
    fillCommonField(builder, item)
  }

  override def insert(item: TaskRecord): Future[String] = {
    Future(insertSync(item))
  }

  def insertSync(item: TaskRecord): String = {
    index(modelToDoc(item))
  }

}
