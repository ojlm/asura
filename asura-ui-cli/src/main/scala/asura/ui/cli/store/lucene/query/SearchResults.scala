package asura.ui.cli.store.lucene.query

import asura.ui.cli.store.lucene.field.FacetField
import org.apache.lucene.search.TopDocs

case class SearchResults(topDocs: TopDocs, facetResults: Map[FacetField, FacetResult])
