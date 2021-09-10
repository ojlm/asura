package asura.ui.cli.store.lucene.query

import asura.ui.cli.store.lucene.Lucene
import asura.ui.cli.store.lucene.field.FacetField
import asura.ui.cli.store.lucene.query.SearchTerm.{GroupedSearchTerm, MatchAll}
import org.apache.lucene.search.TopFieldCollector
import org.apache.lucene.search.highlight.{Highlighter, QueryScorer, SimpleHTMLFormatter}

case class QueryBuilder[T](
                            lucene: Lucene,
                            facets: Set[FacetQuery] = Set.empty,
                            offset: Int = 0,
                            limit: Int = 10,
                            sorts: List[Sort] = Nil,
                            keepScores: Boolean = false,
                            scoreMax: Boolean = false,
                            searchTerms: List[SearchTerm] = Nil,
                            converter: SearchResult => T,
                            highlighting: Option[Highlighting] = None,
                          ) {

  def offset(v: Int): QueryBuilder[T] = copy(offset = v)

  def limit(v: Int): QueryBuilder[T] = copy(limit = v)

  def convert[T](converter: SearchResult => T): QueryBuilder[T] = copy(converter = converter)

  def facet(field: FacetField, limit: Int = 10, path: List[String] = Nil): QueryBuilder[T] = {
    copy(facets = facets + FacetQuery(field, limit, path))
  }

  def keepScores(b: Boolean): QueryBuilder[T] = copy(keepScores = b)

  def scoreMax(b: Boolean): QueryBuilder[T] = copy(scoreMax = b)

  def filter(searchTerms: SearchTerm*): QueryBuilder[T] = {
    copy(searchTerms = this.searchTerms ::: searchTerms.toList)
  }

  def highlight(preTag: String = "<em>", postTag: String = "</em>"): QueryBuilder[T] = {
    copy(highlighting = Some(Highlighting(preTag, postTag)))
  }

  def sort(sort: Sort*): QueryBuilder[T] = copy(sorts = sorts ::: sort.toList)

  def replaceSort(sort: Sort*): QueryBuilder[T] = copy(sorts = sort.toList)

  def build(): SearchTerm = {
    searchTerms match {
      case Nil => MatchAll
      case st :: Nil => st
      case _ => GroupedSearchTerm(0, searchTerms.map(st => (st, Condition.MUST)))
    }
  }

  def search(): PagedResults[T] = {
    val query = build().toLucene(lucene)
    val manager = new DocumentCollector(lucene, this)
    lucene.withSearcherAndTaxonomy(instance => {
      val results = instance.searcher.search(query, manager)
      if (keepScores) {
        TopFieldCollector.populateScores(results.topDocs.scoreDocs, instance.searcher, query)
      }
      val highlighter = highlighting.map {
        case Highlighting(preTag, postTag) =>
          val formatter = new SimpleHTMLFormatter(preTag, postTag)
          new Highlighter(formatter, new QueryScorer(query))
      }
      PagedResults(lucene, this, offset, results, highlighter)
    })
  }

  def delete(): Unit = {
    lucene.delete(build())
  }

}
