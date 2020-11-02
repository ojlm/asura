package asura.app.security

import java.time.Duration

import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.Permissions
import asura.core.es.service.PermissionsService
import asura.core.security._
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}

import scala.concurrent.Future

case class DefaultPermissionAuthProvider(
                                          userCacheCount: Int,
                                          userCacheDuration: Duration,
                                          groupCacheCount: Int,
                                          groupCacheDuration: Duration,
                                          projectCacheCount: Int,
                                          projectCacheDuration: Duration,
                                        ) extends PermissionAuthProvider {

  private val userRolesCache: Cache[String, UserRoles] = Caffeine.newBuilder()
    .maximumSize(userCacheCount).expireAfterWrite(userCacheDuration).build()

  private val groupCache: Cache[String, Seq[PermissionItem]] = Caffeine.newBuilder()
    .maximumSize(groupCacheCount).expireAfterWrite(groupCacheDuration).build()

  private val projectCache: Cache[String, Seq[PermissionItem]] = Caffeine.newBuilder()
    .maximumSize(projectCacheCount).expireAfterWrite(projectCacheDuration).build()

  override def authorize(username: String, group: String, project: Option[String], function: String): Future[AuthResponse] = {
    getUserRoles(username).flatMap(roles => {
      val isAllowed = Permissions.isAllowed(group, project, function, roles)
      if (isAllowed) {
        Future.successful(AuthResponse(isAllowed))
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

  override def getUserRoles(username: String): Future[UserRoles] = {
    val roles = userRolesCache.getIfPresent(username)
    if (null == roles) {
      PermissionsService.getRolesOfUser(username).map(roles => {
        userRolesCache.put(username, roles)
        roles
      })
    } else {
      Future.successful(roles)
    }
  }

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
