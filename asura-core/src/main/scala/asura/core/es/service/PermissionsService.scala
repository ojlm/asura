package asura.core.es.service

import java.util

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, JsonUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.model.QueryPermissions
import asura.core.security.{MemberRoleItem, PermissionItem, UserRoles}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Response
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object PermissionsService extends CommonService with BaseAggregationService {

  val necessaryFields = Seq(FieldKeys.FIELD_GROUP, FieldKeys.FIELD_PROJECT, FieldKeys.FIELD_TYPE,
    FieldKeys.FIELD_USERNAME, FieldKeys.FIELD_ROLE)

  def index(item: Permissions): Future[IndexDocResponse] = {
    val error = validate(item)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.esClient.execute {
        indexInto(Permissions.Index)
          .doc(item)
          .refresh(RefreshPolicy.WAIT_FOR)
      }.map(toIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(Permissions.Index).refresh(RefreshPolicy.WAIT_FOR)
      }.map(_ => toDeleteDocResponse(_))
    }
  }

  def updateDoc(id: String, doc: Permissions): Future[UpdateDocResponse] = {
    if (StringUtils.isEmpty(id) || null == doc) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(id).in(Permissions.Index).doc(JsonUtils.stringify(doc.toUpdateMap))
      }.map(toUpdateDocResponse(_))
    }
  }

  def validate(item: Permissions): ErrorMessage = {
    if (null == item ||
      StringUtils.hasEmpty(item.group, item.`type`, item.username, item.role) ||
      !item.isValidRole()
    ) {
      ErrorMessages.error_InvalidParams
    } else {
      null
    }
  }

  def queryDocs(query: QueryPermissions) = {
    val esQueries = ArrayBuffer[Query]()
    if (query.`type`.equals(Permissions.TYPE_GROUP)) {
      esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
      esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    } else if (query.`type`.equals(Permissions.TYPE_PROJECT)) {
      esQueries += boolQuery().should(
        boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, query.group),
          termQuery(FieldKeys.FIELD_TYPE, Permissions.TYPE_GROUP)
        ),
        boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, query.group),
          termQuery(FieldKeys.FIELD_PROJECT, query.project),
          termQuery(FieldKeys.FIELD_TYPE, Permissions.TYPE_PROJECT)
        )
      )
    }
    if (StringUtils.isNotEmpty(query.username)) esQueries += wildcardQuery(FieldKeys.FIELD_USERNAME, s"${query.username}*")
    EsClient.esClient.execute {
      search(Permissions.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_UPDATED_AT)
    }
  }

  // limit count
  def getRolesOfUser(username: String): Future[UserRoles] = {
    EsClient.esClient.execute {
      search(Permissions.Index)
        .query(boolQuery().must(termQuery(FieldKeys.FIELD_USERNAME, username)))
        .limit(EsConfig.MaxCount)
        .sourceInclude(necessaryFields)
    }.map(res => {
      if (res.isSuccess) {
        val groups = new util.HashMap[String, MemberRoleItem]()
        val projects = new util.HashMap[String, MemberRoleItem]()
        res.result.hits.hits.foreach(hit => {
          val map = hit.sourceAsMap
          val group = map.getOrElse(FieldKeys.FIELD_GROUP, StringUtils.EMPTY).asInstanceOf[String]
          val project = map.getOrElse(FieldKeys.FIELD_PROJECT, StringUtils.EMPTY).asInstanceOf[String]
          val role = map.getOrElse(FieldKeys.FIELD_ROLE, StringUtils.EMPTY).asInstanceOf[String]
          map.getOrElse(FieldKeys.FIELD_TYPE, null) match {
            case Permissions.TYPE_GROUP =>
              groups.put(group, MemberRoleItem(group, project, role))
            case Permissions.TYPE_PROJECT =>
              projects.put(Permissions.projectMapKey(group, project), MemberRoleItem(group, project, role))
            case _ =>
          }
        })
        UserRoles(groups, projects)
      } else {
        throw ErrorMessages.error_EsRequestFail(res).toException
      }
    })
  }

  // limit count
  def getGroupMaintainers(group: String): Future[Seq[PermissionItem]] = {
    EsClient.esClient.execute {
      search(Permissions.Index)
        .query(boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, group),
          termQuery(FieldKeys.FIELD_TYPE, Permissions.TYPE_GROUP),
          termsQuery(FieldKeys.FIELD_ROLE, Seq(Permissions.ROLE_OWNER, Permissions.ROLE_MAINTAINER))
        ))
        .limit(EsConfig.MaxCount)
        .sourceInclude(necessaryFields)
    }.map(res => toItems(res))
  }

  // limit count
  def getProjectMaintainers(group: String, project: String): Future[Seq[PermissionItem]] = {
    EsClient.esClient.execute {
      search(Permissions.Index)
        .query(boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, group),
          termsQuery(FieldKeys.FIELD_PROJECT, project),
          termQuery(FieldKeys.FIELD_TYPE, Permissions.TYPE_PROJECT),
          termsQuery(FieldKeys.FIELD_ROLE, Seq(Permissions.ROLE_OWNER, Permissions.ROLE_MAINTAINER))
        ))
        .limit(EsConfig.MaxCount)
        .sourceInclude(necessaryFields)
    }.map(res => toItems(res))
  }

  private def toItems(res: Response[SearchResponse]): Seq[PermissionItem] = {
    if (res.isSuccess) {
      val items = ArrayBuffer[PermissionItem]()
      res.result.hits.hits.foreach(hit => {
        val map = hit.sourceAsMap
        val group = map.getOrElse(FieldKeys.FIELD_GROUP, StringUtils.EMPTY).asInstanceOf[String]
        val project = map.getOrElse(FieldKeys.FIELD_PROJECT, StringUtils.EMPTY).asInstanceOf[String]
        val username = map.getOrElse(FieldKeys.FIELD_USERNAME, StringUtils.EMPTY).asInstanceOf[String]
        val role = map.getOrElse(FieldKeys.FIELD_ROLE, StringUtils.EMPTY).asInstanceOf[String]
        items += PermissionItem(group, project, username, role)
      })
      items.toSeq
    } else {
      throw ErrorMessages.error_EsRequestFail(res).toException
    }
  }
}
