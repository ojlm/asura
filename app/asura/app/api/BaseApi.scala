package asura.app.api

import akka.util.ByteString
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.JsonUtils
import asura.core.es.EsResponse
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.http.{RequestFailure, RequestSuccess}
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala.Security
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc._

import scala.collection.JavaConverters.asScalaBuffer

trait BaseApi extends Security[CommonProfile] {

  import BaseApi._

  def getProfiles()(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  implicit class JsonToClass(req: Request[String]) {
    def bodyAs[T <: AnyRef](c: Class[T]): T = JsonUtils.parse[T](req.body, c)
  }

  def toActionResult(either: Either[RequestFailure, RequestSuccess[SearchResponse]]): Result = {
    either match {
      case Right(success) => OkApiRes(ApiRes(data = EsResponse.toApiData(success.result)))
      case Left(failure) => OkApiRes(ApiResError(msg = failure.error.reason))
    }
  }
}

object BaseApi {

  object OkApiRes {
    def apply(apiRes: ApiRes): Result = {
      Result(
        header = ResponseHeader(200),
        HttpEntity.Strict(ByteString(JsonUtils.stringify(apiRes)), Some(ContentTypes.JSON))
      )
    }
  }

}
