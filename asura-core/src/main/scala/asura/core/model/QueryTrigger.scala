package asura.core.model

case class QueryTrigger(
                         group: String,
                         project: String,
                         env: String,
                         service: String,
                         enabled: String,
                         text: String
                       ) extends QueryPage
