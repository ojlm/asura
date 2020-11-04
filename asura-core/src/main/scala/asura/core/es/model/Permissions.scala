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
    // 查看分组成员
    val GROUP_MEMBERS_VIEW = "group.members.view"
    // 编辑分组成员
    val GROUP_MEMBERS_EDIT = "group.members.edit"
    // project
    // 创建项目
    val PROJECT_CREATE = "project.create"
    // 删除项目
    val PROJECT_REMOVE = "project.remove"
    // 查看项目详细
    val PROJECT_INFO_VIEW = "project.info.view"
    // 编辑项目详细
    val PROJECT_INFO_EDIT = "project.info.edit"
    // 项目统一查询
    val PROJECT_LIST = "project.list"
    // 产看项目 openapi
    val PROJECT_OPENAPI_VIEW = "project.openapi.view"
    // 编辑项目 openapi
    val PROJECT_OPENAPI_EDIT = "project.openapi.edit"
    // 转移项目
    val PROJECT_TRANSFER = "project.transfer"
    // 查看项目成员
    val PROJECT_MEMBERS_VIEW = "project.members.view"
    // 查看项目成员
    val PROJECT_MEMBERS_EDIT = "project.members.edit"
    // 查看项目报告
    val PROJECT_REPORT_VIEW = "project.report.view"
    // 组件查询列表
    val PROJECT_COMPONENT_LIST = "project.component.list"
    // 组件创建或编辑
    val PROJECT_COMPONENT_EDIT = "project.component.edit"
    // 组件删除
    val PROJECT_COMPONENT_REMOVE = "project.component.remove"
    // 组件创建克隆
    val PROJECT_COMPONENT_CLONE = "project.component.clone"
    // 组件查看详情
    val PROJECT_COMPONENT_VIEW = "project.component.view"
    // 组件执行
    val PROJECT_COMPONENT_EXEC = "project.component.exec"
    // OPENAPI 预览
    val PROJECT_OPENAPI_PREVIEW = "project.openapi.preview"
    // OPENAPI 导入
    val PROJECT_OPENAPI_IMPORT = "project.openapi.import"
    // 组件批量修改标签
    val PROJECT_COMPONENT_BATCH_LABEL = "project.component.batch.label"
    // 组件批量删除
    val PROJECT_COMPONENT_BATCH_REMOVE = "project.component.batch.remove"
    // 组件批量转移
    val PROJECT_COMPONENT_BATCH_TRANSFER = "project.component.batch.transfer"

    val ADMIN_FUNCTIONS = Set(GROUP_CREATE, PROJECT_TRANSFER)
    val GUEST_FUNCTIONS = Set(GROUP_LIST, GROUP_INFO_VIEW, GROUP_PROJECT_LIST, GROUP_JOB_LIST, GROUP_MEMBERS_VIEW,
      PROJECT_LIST, PROJECT_INFO_VIEW, PROJECT_OPENAPI_VIEW, PROJECT_MEMBERS_VIEW,
      PROJECT_COMPONENT_LIST
    )
    val REPORTER_FUNCTIONS = GUEST_FUNCTIONS ++ Set(PROJECT_REPORT_VIEW, PROJECT_COMPONENT_VIEW)
    val DEVELOPER_FUNCTIONS = REPORTER_FUNCTIONS ++ Set(PROJECT_CREATE, PROJECT_COMPONENT_EDIT,
      PROJECT_COMPONENT_CLONE, PROJECT_COMPONENT_REMOVE, PROJECT_COMPONENT_EXEC, PROJECT_OPENAPI_PREVIEW
    )
    val MAINTAINER_FUNCTIONS = DEVELOPER_FUNCTIONS ++ Set(
      PROJECT_REMOVE, GROUP_INFO_EDIT,
      PROJECT_INFO_EDIT, PROJECT_OPENAPI_EDIT,
      PROJECT_COMPONENT_BATCH_LABEL, PROJECT_COMPONENT_BATCH_REMOVE, PROJECT_COMPONENT_BATCH_TRANSFER,
      PROJECT_OPENAPI_IMPORT
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
