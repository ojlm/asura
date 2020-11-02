package asura.core.model

case class QueryJob(
                     var group: String,
                     var project: String,
                     text: String,
                     triggerType: String,
                   ) extends QueryPage
