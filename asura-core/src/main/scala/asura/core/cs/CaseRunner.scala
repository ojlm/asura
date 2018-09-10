package asura.core.cs

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import asura.common.util.StringUtils
import asura.core.CoreConfig._
import asura.core.cs.assertion.engine.HttpResponseAssert
import asura.core.es.model.{Case, KeyValueObject}
import asura.core.es.service.EnvironmentService
import asura.core.http.{HttpContentTypes, HttpEngine}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object CaseRunner {

  val logger = Logger("CaseRunner")

  def test(id: String, cs: Case, context: CaseContext = CaseContext()): Future[CaseResult] = {
    context.eraseCurrentData()
    if (StringUtils.isNotEmpty(cs.env)) {
      EnvironmentService.getEnvById(cs.env)
        .flatMap(env => CaseParser.toHttpRequest(cs, context, env))
        .flatMap(toCaseRequestTuple)
        .flatMap(tuple => {
          if (Option(cs.useProxy).isDefined && cs.useProxy) {
            HttpEngine.singleRequestWithProxy(tuple._1).flatMap(res => {
              Unmarshal(res.entity).to[String].flatMap(resBody => {
                HttpResponseAssert.generateCaseReport(id, cs.assert, res, resBody, tuple._2, context)
              })
            })
          } else {
            HttpEngine.singleRequest(tuple._1).flatMap(res => {
              Unmarshal(res.entity).to[String].flatMap(resBody => {
                HttpResponseAssert.generateCaseReport(id, cs.assert, res, resBody, tuple._2, context)
              })
            })
          }
        })
    } else {
      CaseParser.toHttpRequest(cs, context)
        .flatMap(toCaseRequestTuple)
        .flatMap(tuple => {
          HttpEngine.singleRequest(tuple._1).flatMap(res => {
            Unmarshal(res.entity).to[String].flatMap(resBody => {
              HttpResponseAssert.generateCaseReport(id, cs.assert, res, resBody, tuple._2, context)
            })
          })
        })
    }
  }

  def toCaseRequestTuple(req: HttpRequest): Future[(HttpRequest, CaseRequest)] = {
    Unmarshal(req.entity).to[String].map(reqBody => {
      val mediaType = req.entity.contentType.mediaType.value
      val headers = req.headers
        .map(h => KeyValueObject(h.name(), h.value()))
        .+:(KeyValueObject(HttpContentTypes.KEY_CONTENT_TYPE, mediaType))
      (req, CaseRequest(req.method.value, req.getUri().toString, headers, reqBody))
    })
  }
}
