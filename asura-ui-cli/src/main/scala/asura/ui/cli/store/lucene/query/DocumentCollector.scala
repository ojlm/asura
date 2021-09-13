package asura.ui.cli.store.lucene.query

import java.util

import scala.jdk.CollectionConverters.CollectionHasAsScala

import asura.ui.cli.store.lucene.Lucene
import asura.ui.cli.store.lucene.field.FacetField
import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts
import org.apache.lucene.search.{CollectorManager, TopDocs, TopFieldCollector, Sort => LuceneSort}

class DocumentCollector(lucene: Lucene, query: QueryBuilder[_]) extends CollectorManager[Collectors, SearchResults] {

  val sort: LuceneSort = if (query.sorts.nonEmpty) {
    new LuceneSort(query.sorts.map(_.sortField()): _*)
  } else {
    LuceneSort.RELEVANCE
  }

  override def newCollector(): Collectors = {
    val docMax = math.max(1, lucene.withSearcherAndTaxonomy(_.searcher.getIndexReader.maxDoc()))
    val docLimit = math.min(query.offset + query.limit, docMax)
    val topFieldCollector = TopFieldCollector.create(sort, docLimit, Int.MaxValue)
    val facetsCollector = new FacetsCollector(query.keepScores)
    Collectors(topFieldCollector, facetsCollector)
  }

  override def reduce(collectors: util.Collection[Collectors]): SearchResults = {
    val topDocs = collectors.asScala.collect {
      case Collectors(tfc, _) => tfc.topDocs()
    }.toArray
    val facetsCollector = collectors.asScala.head.facetsCollector
    val docMax = math.max(1, lucene.withSearcherAndTaxonomy(_.searcher.getIndexReader.maxDoc()))
    val docLimit = math.min(query.offset + query.limit, docMax)
    val topFieldDocs = TopDocs.merge(sort, docLimit, topDocs) match {
      case td if query.offset > 0 => new TopDocs(td.totalHits, td.scoreDocs.slice(query.offset, query.offset + query.limit))
      case td => td
    }
    var facetResults = Map.empty[FacetField, FacetResult]
    if (query.facets.nonEmpty) {
      lucene.withSearcherAndTaxonomy(instance => {
        val facets = new FastTaxonomyFacetCounts(instance.taxonomyReader, lucene.facetsConfig, facetsCollector)
        query.facets.foreach(fq => {
          val path = fq.path
          Option(facets.getTopChildren(fq.limit, fq.facet.name, path: _*)) match {
            case Some(r) => {
              val values = if (r.childCount > 0) {
                r.labelValues.toVector.map(lv => FacetResultValue(lv.label, lv.value.intValue()))
              } else {
                Vector.empty
              }
              val updatedValues = values.filterNot(_.value == "$ROOT$")
              val totalCount = updatedValues.map(_.count).sum
              val facetResult = FacetResult(fq.facet, updatedValues, updatedValues.length, totalCount)
              facetResults += fq.facet -> facetResult
            }
            case None => facetResults += fq.facet -> FacetResult(fq.facet, Vector.empty, 0, 0)
          }
        })
      })
    }
    SearchResults(topFieldDocs, facetResults)
  }

}
