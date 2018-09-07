package asura.core.es.service

import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.{FutureUtils, JsonUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{IndexDocResponse, UserProfile}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.Future

object UserProfileService extends CommonService {

  def index(profile: UserProfile): Future[IndexDocResponse] = {
    if (null == profile) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      val (isOK, errMsg) = validate(profile)
      if (!isOK) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.httpClient.execute {
          indexInto(UserProfile.Index / EsConfig.DefaultType).doc(profile).id(profile.username).refresh(RefreshPolicy.WAIT_UNTIL)
        }.map(toIndexDocResponse(_))
      }
    }
  }

  def deleteDoc(id: String): Future[BoolErrorRes] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(UserProfile.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toBoolErrorResFromDelete(_))
    }
  }

  def updateProfile(profile: UserProfile): Future[BoolErrorRes] = {
    if (null == profile || null == profile.username) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        update(profile.username).in(UserProfile.Index / EsConfig.DefaultType).doc(profile.toUpdateMap)
      }.map(toBoolErrorResFromUpdate(_))
    }
  }

  def validate(profile: UserProfile): BoolErrorRes = {
    if (StringUtils.isEmpty(profile.username)) {
      (false, "Empty username")
    } else {
      (true, null)
    }
  }

  def getProfileById(id: String): Future[UserProfile] = {
    if (StringUtils.isEmpty(id)) {
      Future.successful(null)
    } else {
      if (StringUtils.isEmpty(id)) {
        FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
      } else {
        EsClient.httpClient.execute {
          search(UserProfile.Index).query(idsQuery(id)).size(1)
        }.map(either => toSingleClass(either, id)(str => {
          if (StringUtils.isNotEmpty(str)) {
            JsonUtils.parse(str, classOf[UserProfile])
          } else {
            null
          }
        }))
      }
    }
  }
}
