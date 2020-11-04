package asura.app.security

import java.time.Duration

import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.Permissions
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.PermissionsService
import asura.core.security._
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import play.api.Configuration

import scala.concurrent.Future

case class DefaultPermissionAuthProvider(
                                          configuration: Configuration,
                                        ) extends PermissionAuthProvider {

  val ALLOWED = Future.successful(AuthResponse(true))
  val administrators = configuration.getOptional[Seq[String]]("asura.admin").getOrElse(Nil).toSet
  val permissionCheckEnabled = configuration.getOptional[Boolean]("asura.security.permission.enabled").getOrElse(true)

  val userCacheCount: Int = 1000
  val userCacheDuration: Duration = Duration.ofMinutes(2)
  val groupCacheCount: Int = 1000
  val groupCacheDuration: Duration = Duration.ofMinutes(2)
  val projectCacheCount: Int = 1000
  val projectCacheDuration: Duration = Duration.ofMinutes(2)

  private val userRolesCache: Cache[String, UserRoles] = Caffeine.newBuilder()
    .maximumSize(userCacheCount).expireAfterWrite(userCacheDuration).build()

  private val groupCache: Cache[String, Seq[PermissionItem]] = Caffeine.newBuilder()
    .maximumSize(groupCacheCount).expireAfterWrite(groupCacheDuration).build()

  private val projectCache: Cache[String, Seq[PermissionItem]] = Caffeine.newBuilder()
    .maximumSize(projectCacheCount).expireAfterWrite(projectCacheDuration).build()

  override def authorize(username: String, group: String, project: Option[String], function: String): Future[AuthResponse] = {
    if (permissionCheckEnabled) {
      isAdmin(username).flatMap(bIsAdmin => {
        if (bIsAdmin) {
          ALLOWED
        } else {
          if (Functions.ANONYMOUS_FUNCTIONS.contains(function)) {
            ALLOWED
          } else if (Functions.ADMIN_FUNCTIONS.contains(function)) { // need 'admin' role
            Future.successful(AuthResponse(false, Maintainers(Nil, Nil, administrators.toSeq)))
          } else {
            getUserRoles(username).flatMap(roles => {
              val isAllowed = Permissions.isAllowed(group, project, function, roles)
              if (isAllowed) {
                ALLOWED
              } else {
                if (project.isEmpty) {
                  getGroupMaintainers(group).map(items => {
                    AuthResponse(false, Maintainers(items, Nil))
                  })
                } else {
                  val future = for {
                    groups <- getGroupMaintainers(group)
                    projects <- getProjectMaintainers(group, project.get)
                  } yield (groups, projects)
                  future.map(tuple => {
                    AuthResponse(false, Maintainers(tuple._1, tuple._2))
                  })
                }
              }
            })
          }
        }
      })
    } else {
      ALLOWED
    }
  }

  override def getUserRoles(username: String): Future[UserRoles] = {
    val roles = userRolesCache.getIfPresent(username)
    if (null == roles) {
      PermissionsService.getRolesOfUser(username).flatMap(roles => {
        isAdmin(username).map(bIsAdmin => {
          roles.isAdmin = bIsAdmin
          userRolesCache.put(username, roles)
          roles
        })
      })
    } else {
      Future.successful(roles)
    }
  }

  override def isAdmin(username: String) = Future.successful(administrators.contains(username))

  def getGroupMaintainers(group: String): Future[Seq[PermissionItem]] = {
    val cache = groupCache.getIfPresent(group)
    if (null == cache) {
      PermissionsService.getGroupMaintainers(group).map(items => {
        groupCache.put(group, items)
        items
      })
    } else {
      Future.successful(cache)
    }
  }

  def getProjectMaintainers(group: String, project: String): Future[Seq[PermissionItem]] = {
    val cacheKey = Permissions.projectMapKey(group, project)
    val cache = projectCache.getIfPresent(cacheKey)
    if (null == cache) {
      PermissionsService.getProjectMaintainers(group, project).map(items => {
        projectCache.put(cacheKey, items)
        items
      })
    } else {
      Future.successful(cache)
    }
  }
}
