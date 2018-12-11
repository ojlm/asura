package asura.core.cs

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.http.javadsl.model.ContentType
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import asura.core.CoreConfig.materializer
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.assertion.engine.HttpResponseAssert
import asura.core.es.model.Case
import asura.core.http.{HttpContentTypes, HttpEngine}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object CaseRunner {

  val logger = Logger("CaseRunner")

  def test(caseId: String, cs: Case, context: CaseContext = CaseContext()): Future[CaseResult] = {
    implicit val metrics = CaseRuntimeMetrics()
    metrics.start()
    context.eraseCurrentData()
    var options = context.options
    if (null != options) {
      options.caseEnv = cs.env
    } else {
      options = ContextOptions(caseEnv = cs.env)
      context.options = options
    }
    metrics.renderRequestStart()
    context.evaluateOptions().flatMap(_ => {
      CaseParser.toHttpRequest(cs, context)
        .flatMap(toCaseRequestTuple)
        .flatMap(tuple => {
          val env = if (null != context.options) context.options.getUsedEnv() else null
          if (null != env && env.enableProxy) {
            metrics.performRequestStart()
            HttpEngine.singleRequestWithProxy(tuple._1).flatMap(res => {
              Unmarshal(res.entity).to[ByteString].flatMap(resBody => {
                metrics.evalAssertionBegin()
                HttpResponseAssert.generateCaseReport(caseId, cs.assert, res,
                  byteStringToString(resBody, res.entity.getContentType()), tuple._2, context
                )
              })
            })
          } else {
            metrics.performRequestStart()
            HttpEngine.singleRequest(tuple._1).flatMap(res => {
              Unmarshal(res.entity).to[ByteString].flatMap(resBody => {
                metrics.evalAssertionBegin()
                HttpResponseAssert.generateCaseReport(caseId, cs.assert, res,
                  byteStringToString(resBody, res.entity.getContentType()), tuple._2, context
                )
              })
            })
          }
        })
    }).map(result => {
      metrics.evalAssertionEnd()
      metrics.theEnd()
      result.metrics = metrics.toReportItemMetrics()
      result
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

  // Base64 encode if not text
  private def byteStringToString(byteString: ByteString, contentType: ContentType): String = {
    if (contentType.mediaType.binary) {
      new String(Base64.getEncoder.encode(byteString.toByteBuffer.array()))
    } else {
      byteString.decodeString(StandardCharsets.UTF_8)
    }
  }
}
