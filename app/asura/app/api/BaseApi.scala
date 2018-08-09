package asura.app.api

import akka.util.ByteString
import asura.common.model.ApiRes
import asura.common.util.JsonUtils
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala.Security
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc._

import scala.collection.JavaConverters.asScalaBuffer

trait BaseApi extends Security[CommonProfile] {

  implicit class JsonToClass(req: Request[String]) {
    def bodyAs[T <: AnyRef](c: Class[T]): T = JsonUtils.parse[T](req.body, c)
  }

  def getProfiles()(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
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
