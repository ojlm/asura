package asura.core.dubbo

import asura.core.es.model.JobReportDataItem.{DataItemRenderedRequest, DataItemRenderedResponse}
import asura.dubbo.GenericRequest

object RenderedDubboModel {

  case class RenderedDubboRequest(
                                   dubboGroup: String,
                                   interface: String,
                                   method: String,
                                   parameterTypes: Array[String],
                                   args: Array[Object],
                                   address: String,
                                   port: Int,
                                   version: String
                                 ) extends DataItemRenderedRequest

  object RenderedDubboRequest {

    def apply(request: GenericRequest): RenderedDubboRequest = {
      RenderedDubboRequest(
        request.dubboGroup,
        request.interface,
        request.method,
        request.parameterTypes,
        request.args,
        request.address,
        request.port,
        request.version
      )
    }
  }

  case class RenderedDubboResponse(
                                    body: Object
                                  ) extends DataItemRenderedResponse

}
