package asura.ui.cli.store.lucene.query

import asura.ui.cli.store.lucene.field.FacetField

case class FacetQuery(facet: FacetField, limit: Int, path: List[String])
