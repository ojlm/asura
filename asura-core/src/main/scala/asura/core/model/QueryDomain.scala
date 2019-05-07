package asura.core.model

import asura.core.es.model.FieldKeys

case class QueryDomain(
                        names: Seq[String],
                        val tag: String,
                        var date: String,
                        var sortField: String = FieldKeys.FIELD_COUNT,
                      ) extends QueryPage
