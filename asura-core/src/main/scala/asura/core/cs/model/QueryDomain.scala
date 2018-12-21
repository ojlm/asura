package asura.core.cs.model

import asura.core.es.model.FieldKeys

case class QueryDomain(
                        names: Seq[String],
                        var date: String,
                        var sortField: String = FieldKeys.FIELD_COUNT,
                      ) extends QueryPage
