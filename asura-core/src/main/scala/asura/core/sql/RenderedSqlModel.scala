package asura.core.sql

import asura.core.es.model.JobReportDataItem.{DataItemRenderedRequest, DataItemRenderedResponse}

object RenderedSqlModel {

  case class RenderedSqlRequest(
                                 host: String,
                                 port: Int,
                                 username: String,
                                 database: String,
                                 var table: String,
                                 sql: String,
                               ) extends DataItemRenderedRequest

  case class RenderedSqlResponse(
                                  body: Object
                                ) extends DataItemRenderedResponse

}
