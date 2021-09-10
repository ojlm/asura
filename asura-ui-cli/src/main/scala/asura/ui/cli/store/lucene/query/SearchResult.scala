package asura.ui.cli.store.lucene.query

import asura.ui.cli.store.lucene.Lucene
import asura.ui.cli.store.lucene.field.Field
import asura.ui.cli.store.lucene.query.SearchResult.HighlightedResult
import org.apache.lucene.document.Document
import org.apache.lucene.search.ScoreDoc

class SearchResult(lucene: Lucene, search: PagedResults[_], scoreDoc: ScoreDoc) {

  lazy val doc: Document = lucene.withSearcherAndTaxonomy(_.searcher.doc(scoreDoc.doc))

  def apply[T](field: Field[T]): T = field.support.fromLucene(doc.getFields(field.name).toList)

  def id: Int = scoreDoc.doc

  def score: Float = scoreDoc.score

  def shardIndex: Int = scoreDoc.shardIndex

  def highlighting[T](field: Field[T]): List[HighlightedResult] = {
    search.highlighter match {
      case Some(highlighter) =>
        val text = doc.get(field.name)
        val stream = lucene.analyzer.tokenStream(field.name, text)
        val mergeContiguousFragments = false
        val maxNumFragments = 10
        val fragments = highlighter.getBestTextFragments(stream, text, mergeContiguousFragments, maxNumFragments)
        fragments.toList.collect {
          case frag if frag.getScore > 0.0f => HighlightedResult(frag.toString, frag.getScore)
        }
      case None => Nil
    }
  }

}

object SearchResult {
  case class HighlightedResult(content: String, score: Float, preTag: String = "<em>", postTag: String = "</em>") {
    lazy val fragment: String = content.split('\n').find(_.indexOf(preTag) != -1).getOrElse(content)
    lazy val word: String = {
      val start = fragment.indexOf(preTag)
      val end = fragment.indexOf(postTag)
      assert(start != -1, s"Unable to find emphasis start tag in fragment ($fragment).")
      assert(end != -1, s"Unable to find emphasis end tag in fragment ($fragment).")
      fragment.substring(start + 4, end)
    }
  }
}
