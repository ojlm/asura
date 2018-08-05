package asura.app.api

import asura.common.model.{ApiRes, ApiResError}
import asura.core.es.EsResponse
import asura.core.es.model.Group
import asura.core.es.service.GroupService
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

@Singleton
class GroupApi @Inject()(implicit exec: ExecutionContext)
  extends BaseApi {

  def getById(id: String) = Action.async {
    GroupService.getById(id).map { res =>
      res match {
        case Left(value) => OkApiRes(ApiResError(value.error.reason))
        case Right(value) => OkApiRes(ApiRes(data = EsResponse.toSingleApiData(value.result)))
      }
    }
  }

  def put() = Action(parse.tolerantText).async { req =>
    val group = req.bodyAs(classOf[Group])
    GroupService.index(group).map { res =>
      OkApiRes(ApiRes(data = res))
    }
  }
}
