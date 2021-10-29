package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.TaskRecord

trait TaskRecordOps {

  def insert(item: TaskRecord): Future[String]

}

object TaskRecordOps {

}
