package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.EsConfig
import asura.core.security.UserRoles
import com.sksamuel.elastic4s.requests.mappings.{KeywordField, MappingDefinition, ObjectField}
import net.minidev.json.annotate.JsonIgnore

import scala.collection.mutable

case class Permissions(
                        summary: String = null,
                        description: String = null,
                        var group: String,
                        var project: String,
                        var `type`: String,
                        username: String,
                        role: String,
                        data: Map[String, Any] = null,
                        var creator: String = null,
                        var createdAt: String = null,
                        var updatedAt: String = null,
                      ) extends BaseIndex {

  override def toUpdateMap: Map[String, Any] = {
    val m = mutable.Map[String, Any]()
    checkCommFieldsToUpdate(m)
    if (Permissions.isValidRole(role)) {
      m += (FieldKeys.FIELD_ROLE -> role)
    }
    if (null != data) {
      m += (FieldKeys.FIELD_DATA -> data)
    }
    m.toMap
  }

  @JsonIgnore
  def isValidRole(): Boolean = Permissions.isValidRole(role)
}

object Permissions extends IndexSetting {

  val Index: String = s"${EsConfig.IndexPrefix}permissions"
  override val shards: Int = 5
  override val replicas: Int = 1
  val mappings: MappingDefinition = MappingDefinition(
    BaseIndex.fieldDefinitions ++ Seq(
      KeywordField(name = FieldKeys.FIELD_GROUP),
      KeywordField(name = FieldKeys.FIELD_PROJECT),
      KeywordField(name = FieldKeys.FIELD_TYPE),
      KeywordField(name = FieldKeys.FIELD_USERNAME),
      KeywordField(name = FieldKeys.FIELD_ROLE),
      ObjectField(name = FieldKeys.FIELD_DATA, dynamic = Some("false")),
    )
  )

  val TYPE_GROUP = "group"
  val TYPE_PROJECT = "project"

  // roles
  val ROLE_OWNER = "owner"
  val ROLE_MAINTAINER = "maintainer"
  val ROLE_DEVELOPER = "developer"
  val ROLE_REPORTER = "reporter"
  val ROLE_GUEST = "guest"

  // functions
  object Functions {
    // group
    // 创建分组
    val GROUP_CREATE = "group.created"
    // 删除分组
    val GROUP_REMOVE = "group.remove"
    // 分组列表查询
    val GROUP_LIST = "group.list"
    // 查看分组详情
    val GROUP_INFO_VIEW = "group.info.view"
    // 编辑分组信息
    val GROUP_INFO_EDIT = "group.info.edit"
    // 分组项目列表
    val GROUP_PROJECT_LIST = "group.project.list"
    // 分组任务列表
    val GROUP_JOB_LIST = "group.job.list"
    val GROUP_MEMBERS_VIEW = "group.members.view"
    val GROUP_MEMBERS_EDIT = "group.members.edit"
    // project
    val PROJECT_CREATE = "project.create"
    val PROJECT_REMOVE = "project.remove"
    val PROJECT_INFO_VIEW = "project.info.view"
    val PROJECT_INFO_EDIT = "project.info.edit"
    val PROJECT_MEMBERS_VIEW = "project.members.view"
    val PROJECT_MEMBERS_EDIT = "project.members.edit"
    val PROJECT_REPORT_VIEW = "project.report.view"
    val PROJECT_COMPONENT_LIST = "project.component.list"
    val PROJECT_COMPONENT_VIEW = "project.component.view"
    val PROJECT_COMPONENT_EDIT = "project.component.edit"
    val PROJECT_COMPONENT_EXEC = "project.component.exec"

    val ADMIN_FUNCTIONS = Set(GROUP_CREATE)
    val GUEST_FUNCTIONS = Set(GROUP_LIST, GROUP_INFO_VIEW, GROUP_PROJECT_LIST, GROUP_JOB_LIST, GROUP_MEMBERS_VIEW,
      PROJECT_INFO_VIEW, PROJECT_MEMBERS_VIEW, PROJECT_COMPONENT_LIST, PROJECT_COMPONENT_VIEW
    )
    val REPORTER_FUNCTIONS = GUEST_FUNCTIONS ++ Set(PROJECT_REPORT_VIEW)
    val DEVELOPER_FUNCTIONS = REPORTER_FUNCTIONS ++ Set(PROJECT_COMPONENT_EDIT, PROJECT_COMPONENT_EXEC)
    val MAINTAINER_FUNCTIONS = DEVELOPER_FUNCTIONS ++ Set(
      PROJECT_REMOVE, GROUP_INFO_EDIT, GROUP_MEMBERS_EDIT,
      PROJECT_INFO_EDIT, PROJECT_MEMBERS_EDIT
    )
    val OWNER_FUNCTIONS = MAINTAINER_FUNCTIONS ++ Set(GROUP_REMOVE)

    // 'anonymous' user is not the member of project or group, for now same with 'guest'
    val ANONYMOUS_FUNCTIONS = GUEST_FUNCTIONS
  }

  // misc
  val ROLE_SCORES = Map(ROLE_OWNER -> 5, ROLE_MAINTAINER -> 4, ROLE_DEVELOPER -> 3, ROLE_REPORTER -> 2, ROLE_GUEST -> 1)
  val SCORE_ROLES = Map(5 -> ROLE_OWNER, 4 -> ROLE_MAINTAINER, 3 -> ROLE_DEVELOPER, 2 -> ROLE_REPORTER, 1 -> ROLE_GUEST)

  def isValidRole(role: String): Boolean = {
    role match {
      case ROLE_OWNER | ROLE_MAINTAINER | ROLE_DEVELOPER | ROLE_REPORTER | ROLE_GUEST => true
      case _ => false
    }
  }

  def isAllowed(group: String, project: Option[String], function: String, roles: UserRoles): Boolean = {
    if (StringUtils.hasEmpty(group, function) || null == roles) {
      false
    } else {
      val role = getMostPowerfulRole(group, project, roles)
      if (null != role) {
        role match {
          case ROLE_GUEST => Functions.GUEST_FUNCTIONS.contains(role)
          case ROLE_REPORTER => Functions.REPORTER_FUNCTIONS.contains(role)
          case ROLE_DEVELOPER => Functions.DEVELOPER_FUNCTIONS.contains(role)
          case ROLE_MAINTAINER => Functions.MAINTAINER_FUNCTIONS.contains(role)
          case ROLE_OWNER => Functions.OWNER_FUNCTIONS.contains(role)
          case _ => false
        }
      } else {
        false
      }
    }
  }

  def getMostPowerfulRole(group: String, project: Option[String], roles: UserRoles): String = {
    var score = 0
    if (null != roles.groups) {
      val item = roles.groups.get(group)
      if (null != item) {
        val tmpScore = ROLE_SCORES.getOrElse(item.role, 0)
        if (tmpScore > score) score = tmpScore
      }
    }
    if (project.nonEmpty && null != roles.projects) {
      val item = roles.projects.get(project.get)
      if (null != item) {
        val tmpScore = ROLE_SCORES.getOrElse(item.role, 0)
        if (tmpScore > score) score = tmpScore
      }
    }
    SCORE_ROLES.getOrElse(score, null)
  }

  @inline
  def projectMapKey(group: String, project: String) = s"$group/$project"
}
