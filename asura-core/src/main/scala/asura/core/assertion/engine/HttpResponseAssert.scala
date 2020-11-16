package asura.core.assertion.engine

import java.util.Collections

import akka.http.scaladsl.model.HttpResponse
import asura.core.http.{HeaderUtils, HttpResult, RenderedHttpRequest, RenderedHttpResponse}
import asura.core.runtime.RuntimeContext
import asura.core.util.JsonPathUtils

import scala.concurrent.Future

object HttpResponseAssert {

  val KEY_STATUS = "status"
  val KEY_HEADERS = "headers"
  val KEY_ENTITY = "entity"

  /** use java type system because of json path library */
  def generateHttpReport(
                          docId: String,
                          assert: Map[String, Any],
                          response: HttpResponse,
                          entity: String,
                          renderedRequest: RenderedHttpRequest,
                          runtimeContext: RuntimeContext
                        ): Future[HttpResult] = {
    var isJson = false
    runtimeContext.setCurrentStatus(response.status.intValue())
    val headers = new java.util.ArrayList[java.util.Map[String, String]]()
    response.headers.foreach(header => {
      headers.add(Collections.singletonMap(header.name(), header.value()))
      if (HeaderUtils.isApplicationJson(header)) {
        isJson = true
      }
    })
    runtimeContext.setCurrentHeaders(headers)
    if (isJson) {
      val entityDoc = JsonPathUtils.parse(entity)
      runtimeContext.setCurrentEntity(entityDoc)
    } else {
      try {
        val entityDoc = JsonPathUtils.parse(entity)
        runtimeContext.setCurrentEntity(entityDoc)
      } catch {
        case _: Throwable =>
          runtimeContext.setCurrentEntity(entity)
      }
    }
    val caseResponse = RenderedHttpResponse(
      response.status.intValue(),
      response.status.reason(),
      headers,
      {
        val mediaType = response.entity.getContentType().mediaType
        s"${mediaType.mainType}/${mediaType.subType}"
      },
      entity
    )
    HttpResult.eval(docId, assert, runtimeContext, renderedRequest, caseResponse)
  }
}
