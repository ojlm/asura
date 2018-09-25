package asura.core.cs

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import asura.core.CoreConfig.materializer
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.assertion.engine.HttpResponseAssert
import asura.core.es.model.Case
import asura.core.http.{HttpContentTypes, HttpEngine}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object CaseRunner {

  val logger = Logger("CaseRunner")

  def test(id: String, cs: Case, context: CaseContext = CaseContext()): Future[CaseResult] = {
    context.eraseCurrentData()
    var options = context.options
    if (null != options) {
      options.caseEnv = cs.env
    } else {
      options = ContextOptions(caseEnv = cs.env)
      context.options = options
    }
    context.evaluateOptions().flatMap(_ => {
      CaseParser.toHttpRequest(cs, context)
        .flatMap(toCaseRequestTuple)
        .flatMap(tuple => {
          val env = if (null != context.options) context.options.getUsedEnv() else null
          if (null != env && env.enableProxy) {
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
    })
  }

  def toCaseRequestTuple(req: HttpRequest): Future[(HttpRequest, CaseRequest)] = {
    Unmarshal(req.entity).to[String].map(reqBody => {
      val mediaType = req.entity.contentType.mediaType.value
      val headers = scala.collection.mutable.HashMap[String, String]()
      req.headers.foreach(h => headers += (h.name() -> h.value()))
      headers += (HttpContentTypes.KEY_CONTENT_TYPE -> mediaType)
      (req, CaseRequest(req.method.value, req.getUri().toString, headers, reqBody))
    })
  }
}
