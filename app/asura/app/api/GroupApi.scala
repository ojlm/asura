package asura.app.api

import asura.app.api.model.Message
import asura.core.es.model.Group
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json

@Singleton
class GroupApi @Inject()()
  extends BaseApi {

  def getById(id: String) = Action {
    Ok(id)
  }

  def put() = Action(parse.tolerantText) { req =>
    implicit val value = Json.format[Group]
    val body = req.body
    Ok(body)
  }
}
