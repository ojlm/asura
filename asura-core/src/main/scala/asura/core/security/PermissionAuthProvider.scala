package asura.core.security

import scala.concurrent.Future

trait PermissionAuthProvider {

  def authorize(
                 username: String,
                 group: String,
                 project: Option[String],
                 function: String,
               ): Future[AuthResponse]

  def getUserRoles(username: String): Future[UserRoles]

  def isAdmin(username: String): Future[Boolean]
}
