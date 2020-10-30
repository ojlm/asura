package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.{JsonUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsResponse}
import asura.core.model.QueryUser
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query

import scala.collection.Iterable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object UserProfileService extends CommonService {

  def index(profile: UserProfile): Future[IndexDocResponse] = {
    if (null == profile) {
      ErrorMessages.error_EmptyRequestBody.toFutureFail
    } else {
      val error = validate(profile)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          indexInto(UserProfile.Index).doc(profile).id(profile.username).refresh(RefreshPolicy.WAIT_FOR)
        }.map(toIndexDocResponse(_))
      }
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        delete(id).from(UserProfile.Index).refresh(RefreshPolicy.WAIT_FOR)
      }.map(toDeleteDocResponse(_))
    }
  }

  def updateDoc(profile: UserProfile): Future[UpdateDocResponse] = {
    if (null == profile || null == profile.username) {
      ErrorMessages.error_EmptyUsername.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(profile.username).in(UserProfile.Index).doc(profile.toUpdateMap)
      }.map(toUpdateDocResponse(_))
    }
  }

  def queryDoc(query: QueryUser) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += boolQuery().should(
        wildcardQuery(FieldKeys.FIELD_USERNAME, s"${query.text}*"),
        wildcardQuery(FieldKeys.FIELD_NICKNAME, s"${query.text}*"),
      )
    }
    EsClient.esClient.execute {
      search(UserProfile.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
    }
  }

  def validate(profile: UserProfile): ErrorMessage = {
    if (StringUtils.isEmpty(profile.username)) {
      ErrorMessages.error_EmptyUsername
    } else {
      null
    }
  }

  def getProfileById(username: String): Future[UserProfile] = {
    if (StringUtils.isEmpty(username)) {
      ErrorMessages.error_EmptyUsername.toFutureFail
    } else {
      EsClient.esClient.execute {
        search(UserProfile.Index).query(idsQuery(username)).size(1).sourceExclude(defaultExcludeFields)
      }.map(response => toSingleClass(response, username)(str => {
        if (StringUtils.isNotEmpty(str)) {
          JsonUtils.parse(str, classOf[UserProfile])
        } else {
          null
        }
      }))
    }
  }

  def getByIdsAsRawMap(ids: Iterable[String]) = {
    if (null != ids && ids.nonEmpty) {
      EsClient.esClient.execute {
        search(UserProfile.Index).query(idsQuery(ids)).size(ids.size).sourceExclude(defaultExcludeFields)
      }.map(res => {
        if (res.isSuccess) EsResponse.toIdMap(res.result) else Map.empty
      })
    } else {
      Future.successful(Map.empty)
    }
  }
}
