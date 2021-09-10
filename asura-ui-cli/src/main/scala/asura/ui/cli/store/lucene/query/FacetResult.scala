package asura.ui.cli.store.lucene.query

import asura.ui.cli.store.lucene.field.FacetField

case class FacetResult(
                        field: FacetField,
                        values: Vector[FacetResultValue],
                        childCount: Int,
                        totalCount: Int,
                      )
