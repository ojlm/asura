package asura.app.api

import akka.util.ByteString
import asura.common.model.ApiRes
import asura.common.util.JsonUtils
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc.{InjectedController, Request, ResponseHeader, Result}


trait BaseApi extends InjectedController {

  implicit class JsonToClass(req: Request[String]) {
    def bodyAs[T <: AnyRef](c: Class[T]): T = JsonUtils.parse[T](req.body, c)
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
