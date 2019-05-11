package asura.core.sql

import asura.core.es.model.JobReportDataItem.{ReportDataItemRequest, ReportDataItemResponse}

object SqlReportModel {

  case class SqlRequestReportModel(
                                    val host: String,
                                    val port: Int,
                                    val username: String,
                                    val database: String,
                                    var table: String,
                                    val sql: String,
                                  ) extends ReportDataItemRequest

  case class SqlResponseReportModel(
                                     body: Object
                                   ) extends ReportDataItemResponse

}
