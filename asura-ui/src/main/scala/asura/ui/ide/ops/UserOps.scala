package asura.ui.ide.ops

import scala.concurrent.Future

import asura.ui.ide.model.UserPreference

trait UserOps {

  def getPreference(username: String): Future[UserPreference]

  def insert(item: UserPreference): Future[Long]

}

object UserOps {

}
