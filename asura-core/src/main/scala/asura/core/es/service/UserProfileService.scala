package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.{JsonUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{DeleteDocResponse, IndexDocResponse, UpdateDocResponse, UserProfile}
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.collection.Iterable
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
          indexInto(UserProfile.Index / EsConfig.DefaultType).doc(profile).id(profile.username).refresh(RefreshPolicy.WAIT_UNTIL)
        }.map(toIndexDocResponse(_))
      }
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        delete(id).from(UserProfile.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def updateProfile(profile: UserProfile): Future[UpdateDocResponse] = {
    if (null == profile || null == profile.username) {
      ErrorMessages.error_EmptyUsername.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(profile.username).in(UserProfile.Index / EsConfig.DefaultType).doc(profile.toUpdateMap)
      }.map(toUpdateDocResponse(_))
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
