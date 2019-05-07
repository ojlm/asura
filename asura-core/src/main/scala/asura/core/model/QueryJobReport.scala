package asura.core.model

case class QueryJobReport(
                           scheduler: String,
                           group: String,
                           project: String,
                           classAlias: String,
                           result: String,
                           jobId: String,
                           timeStart: String,
                           timeEnd: String,
                           text: String,
                           `type`: String,
                         ) extends QueryPage
