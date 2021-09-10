package asura.ui.cli.store.lucene.query

import asura.ui.cli.store.lucene.Lucene
import asura.ui.cli.store.lucene.field.FacetField
import asura.ui.cli.store.lucene.query.PagedResults.{PagedEntriesIterator, PagedResultsIterator}
import org.apache.lucene.search.TotalHits
import org.apache.lucene.search.highlight.Highlighter

case class PagedResults[T](
                            lucene: Lucene,
                            query: QueryBuilder[T],
                            offset: Int,
                            searchResults: SearchResults,
                            highlighter: Option[Highlighter],
                          ) {

  lazy val results: Vector[SearchResult] = searchResults.topDocs.scoreDocs.toVector.map(doc => new SearchResult(lucene, this, doc))
  lazy val entries: Vector[T] = results.map(query.converter)

  def apply(index: Int): T = entries(index)

  def pageSize: Int = query.limit

  def pageIndex: Int = offset / pageSize

  def pages: Int = math.ceil(total.toDouble / pageSize.toDouble).toInt

  def total: Long = searchResults.topDocs.totalHits.value

  def totalRelation: TotalHits.Relation = searchResults.topDocs.totalHits.relation

  def facets: Map[FacetField, FacetResult] = searchResults.facetResults

  def facet(field: FacetField): Option[FacetResult] = facets.get(field)

  def page(index: Int): PagedResults[T] = query.offset(pageSize * index).search()

  def hasNextPage: Boolean = ((pageIndex + 1) * pageSize) < total

  def hasPreviousPage: Boolean = offset > 0

  def nextPage(): Option[PagedResults[T]] = if (hasNextPage) Some(page(pageIndex + 1)) else None

  def previousPage(): Option[PagedResults[T]] = if (hasPreviousPage) Some(page(pageIndex - 1)) else None

  def pagedResultsIterator: Iterator[SearchResult] = new PagedResultsIterator(this)

  def pagedEntriesIterator: Iterator[T] = new PagedEntriesIterator[T](this)

}

object PagedResults {

  class PagedResultsIterator(var pagedResults: PagedResults[_]) extends Iterator[SearchResult] {

    var results = pagedResults.results

    override def hasNext: Boolean = {
      results.nonEmpty || pagedResults.hasNextPage
    }

    override def next(): SearchResult = {
      if (results.nonEmpty) {
        try {
          results.head
        } finally {
          results = results.tail
        }
      } else if (pagedResults.hasNextPage) {
        pagedResults = pagedResults.nextPage().getOrElse(throw new RuntimeException("There is no more pages"))
        results = pagedResults.results
        next()
      } else {
        throw new NullPointerException("No more results")
      }
    }

  }

  class PagedEntriesIterator[T](var pagedResults: PagedResults[T]) extends Iterator[T] {

    var entries = pagedResults.entries

    override def hasNext: Boolean = {
      entries.nonEmpty || pagedResults.hasNextPage
    }

    override def next(): T = {
      if (entries.nonEmpty) {
        try {
          entries.head
        } finally {
          entries = entries.tail
        }
      } else if (pagedResults.hasNextPage) {
        pagedResults = pagedResults.nextPage().getOrElse(throw new RuntimeException("There is no more pages"))
        entries = pagedResults.entries
        next()
      } else {
        throw new NullPointerException("No more results")
      }
    }

  }

}
