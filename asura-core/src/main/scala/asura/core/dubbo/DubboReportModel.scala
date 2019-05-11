package asura.core.dubbo

import asura.core.es.model.JobReportDataItem.{ReportDataItemRequest, ReportDataItemResponse}
import asura.dubbo.GenericRequest

object DubboReportModel {

  case class DubboRequestReportModel(
                                      dubboGroup: String,
                                      interface: String,
                                      method: String,
                                      parameterTypes: Array[String],
                                      args: Array[Object],
                                      address: String,
                                      port: Int,
                                      version: String
                                    ) extends ReportDataItemRequest

  object DubboRequestReportModel {

    def apply(request: GenericRequest): DubboRequestReportModel = {
      DubboRequestReportModel(
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

  case class DubboResponseReportModel(
                                       body: Object
                                     ) extends ReportDataItemResponse

}
