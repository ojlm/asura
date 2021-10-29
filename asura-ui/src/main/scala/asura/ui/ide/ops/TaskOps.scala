package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.Task

trait TaskOps {

  def insert(item: Task): Future[String]

}

object TaskOps {

}
