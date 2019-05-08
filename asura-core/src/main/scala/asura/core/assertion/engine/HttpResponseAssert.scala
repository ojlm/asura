package asura.core.assertion.engine

import akka.http.scaladsl.model.HttpResponse
import asura.core.http.{HeaderUtils, HttpRequestReportModel, HttpResponseReportModel, HttpResult}
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
                          caseRequest: HttpRequestReportModel,
                          caseContext: RuntimeContext
                        ): Future[HttpResult] = {
    var isJson = false
    caseContext.setCurrentStatus(response.status.intValue())
    val headers = new java.util.HashMap[String, String]()
    response.headers.foreach(header => {
      headers.put(header.name(), header.value())
      if (HeaderUtils.isApplicationJson(header)) {
        isJson = true
      }
    })
    caseContext.setCurrentHeaders(headers)
    if (isJson) {
      val entityDoc = JsonPathUtils.parse(entity)
      caseContext.setCurrentEntity(entityDoc)
    } else {
      try {
        val entityDoc = JsonPathUtils.parse(entity)
        caseContext.setCurrentEntity(entityDoc)
      } catch {
        case _: Throwable =>
          caseContext.setCurrentEntity(entity)
      }
    }
    import scala.collection.JavaConverters.mapAsScalaMap
    val caseResponse = HttpResponseReportModel(
      response.status.intValue(),
      response.status.reason(),
      mapAsScalaMap(headers), {
        val mediaType = response.entity.getContentType().mediaType
        s"${mediaType.mainType}/${mediaType.subType}"
      },
      entity
    )
    HttpResult.eval(docId, response, assert, caseContext, caseRequest, caseResponse)
  }
}
