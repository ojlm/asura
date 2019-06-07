package asura.app.api

import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.EsResponse
import asura.core.es.model.FieldKeys
import asura.core.es.service.UserProfileService
import asura.play.api.BaseApi.OkApiRes
import asura.play.api.{BaseApi => PlayBaseApi}
import com.sksamuel.elastic4s.http.Response
import com.sksamuel.elastic4s.http.search.SearchResponse
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait BaseApi extends PlayBaseApi {

  def toActionResultFromEs(response: Response[SearchResponse], hasId: Boolean = true): Result = {
    if (response.isSuccess) {
      OkApiRes(ApiRes(data = EsResponse.toApiData(response.result, hasId)))
    } else {
      OkApiRes(ApiResError(msg = response.error.reason))
    }
  }

  implicit class EsListResponseToOkResult(f: Future[Response[SearchResponse]])
                                         (implicit ec: ExecutionContext) {
    def toOkResultByEsList(hasId: Boolean = true) = f.map(toActionResultFromEs(_, hasId))
  }

  def toActionResultWithSingleDataFromEs(
                                          response: Response[SearchResponse],
                                          id: String, hasId: Boolean = true
                                        )(implicit request: RequestHeader): Result = {
    if (response.isSuccess) {
      if (response.result.nonEmpty) {
        OkApiRes(ApiRes(data = EsResponse.toSingleApiData(response.result, hasId)))
      } else {
        OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_IdNonExists.name, id)))
      }
    } else {
      OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_EsRequestFail(response).name)))
    }
  }

  implicit class EsSingleResponseToOkResult(f: Future[Response[SearchResponse]])
                                           (implicit ec: ExecutionContext, implicit val request: RequestHeader) {
    def toOkResultByEsOneDoc(id: String, hasId: Boolean = true) = f.map(toActionResultWithSingleDataFromEs(_, id, hasId))
  }

  def withSingleUserProfile(docId: String, response: Response[SearchResponse])
                           (implicit ec: ExecutionContext, req: Request[Any]): Future[Result] = {
    if (response.isSuccess) {
      if (response.result.nonEmpty) {
        val hit = response.result.hits.hits(0)
        val creator = hit.sourceAsMap.getOrElse(FieldKeys.FIELD_CREATOR, StringUtils.EMPTY).asInstanceOf[String]
        if (StringUtils.isNotEmpty(creator)) {
          UserProfileService.getProfileById(creator).map(userProfile => {
            OkApiRes(ApiRes(data =
              EsResponse.toSingleApiData(response.result, true) + ("_creator" -> userProfile)
            ))
          })
        } else {
          Future.successful(OkApiRes(ApiRes(data = EsResponse.toSingleApiData(response.result, true))))
        }
      } else {
        Future.successful(OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_IdNonExists.name, docId))))
      }
    } else {
      Future.successful(OkApiRes(ApiResError(getI18nMessage(ErrorMessages.error_EsRequestFail(response).name))))
    }
  }
}
