package asura.ui.cli.store

import asura.ui.cli.store.lucene.field.{FacetValue, Field, FieldValue, SpatialPoint}
import asura.ui.cli.store.lucene.query.SearchTerm._
import asura.ui.cli.store.lucene.query.{Condition, SearchTerm}
import org.apache.lucene.queries.mlt.MoreLikeThis

package object lucene extends ImplicitField {

  implicit def fv2SearchTerm[T](fv: FieldValue[T]): SearchTerm = fv.field.support.searchTerm(fv)

  implicit def string2ParsableSearchTerm(value: String): SearchTerm = parse(value)

  def matchAll(): SearchTerm = MatchAll

  def parse(field: Field[String], value: String): ParsableSearchTerm = parse(field, value, false)

  def parse(field: Field[String], value: String, allowLeadingWildcard: Boolean): ParsableSearchTerm = ParsableSearchTerm(Some(field), value, allowLeadingWildcard)

  def parse(value: String): ParsableSearchTerm = parse(value, false)

  def parse(value: String, allowLeadingWildcard: Boolean): ParsableSearchTerm = ParsableSearchTerm(None, value, allowLeadingWildcard)

  def parseFuzzy(text: String, field: Option[Field[String]] = None): ParsableSearchTerm = {
    val queryText = text.filterNot(Lucene.specialCharacters.contains).split(' ').flatMap {
      case word if word.trim.isEmpty => None
      case word => Some(s"$word~")
    }.mkString("(", "AND", ")")
    ParsableSearchTerm(field, queryText, false)
  }

  def parseQuery(
                  text: String,
                  field: Option[Field[String]] = None,
                  allowLeadingWildcard: Boolean = false,
                  includeFuzzy: Boolean = false,
                ): ParsableSearchTerm = {

    val queryText = text.split(' ').map {
      case word if Lucene.isLuceneWord(word) => word
      case word if !allowLeadingWildcard && !includeFuzzy => s"$word*"
      case word => {
        val b = new StringBuilder("(")
        // starts with, boosted
        b.append(word)
        b.append("*^4")
        if (allowLeadingWildcard) {
          // leading wildcard
          b.append(" OR *")
          b.append(word)
          b.append("*")
        }
        if (includeFuzzy) {
          // fuzzy matches
          b.append(" OR ")
          b.append(word)
          b.append("~")
        }
        b.append(")")
        b.toString()
      }
    }.mkString("(", " ", ")")
    ParsableSearchTerm(field, queryText, allowLeadingWildcard)
  }

  def term(value: String): TermSearchTerm = TermSearchTerm(None, value)

  def term(fv: FieldValue[String]): TermSearchTerm = TermSearchTerm(Some(fv.field), fv.value.toLowerCase)

  def exact[T](fv: FieldValue[T]): SearchTerm = fv.field.support.searchTerm(fv)

  def intRange(field: Field[Int], start: Int, end: Int): SearchTerm = RangeIntSearchTerm(field, start, end)

  def longRange(field: Field[Long], start: Long, end: Long): SearchTerm = RangeLongSearchTerm(field, start, end)

  def doubleRange(field: Field[Double], start: Double, end: Double): SearchTerm = RangeDoubleSearchTerm(field, start, end)

  def intSet(field: Field[Int], set: Seq[Int]): SearchTerm = SetIntSearchTerm(field, set)

  def longSet(field: Field[Long], set: Seq[Long]): SearchTerm = SetLongSearchTerm(field, set)

  def doubleSet(field: Field[Double], set: Seq[Double]): SearchTerm = SetDoubleSearchTerm(field, set)

  def regexp(value: String): RegexpSearchTerm = RegexpSearchTerm(None, value)

  def regexp(fv: FieldValue[String]): RegexpSearchTerm = RegexpSearchTerm(Some(fv.field), fv.value)

  def wildcard(value: String): WildcardSearchTerm = WildcardSearchTerm(None, value)

  def wildcard(fv: FieldValue[String]): WildcardSearchTerm = WildcardSearchTerm(Some(fv.field), fv.value)

  def fuzzy(value: String): FuzzySearchTerm = FuzzySearchTerm(None, value)

  def fuzzy(fv: FieldValue[String]): FuzzySearchTerm = FuzzySearchTerm(Some(fv.field), fv.value)

  def phrase(fv: FieldValue[String]): PhraseSearchTerm = phrase(fv, 0)

  def phrase(fv: FieldValue[String], slop: Int): PhraseSearchTerm = PhraseSearchTerm(Some(fv.field), fv.value, slop)

  def phrase(value: String): PhraseSearchTerm = phrase(value, 0)

  def phrase(value: String, slop: Int): PhraseSearchTerm = PhraseSearchTerm(None, value, slop)

  def mltFullText(
                   value: String,
                   minTermFreq: Int = MoreLikeThis.DEFAULT_MIN_TERM_FREQ,
                   minDocFreq: Int = MoreLikeThis.DEFAULT_MIN_DOC_FREQ,
                   maxDocFreq: Int = MoreLikeThis.DEFAULT_MAX_DOC_FREQ,
                   boost: Boolean = MoreLikeThis.DEFAULT_BOOST,
                   minWordLen: Int = MoreLikeThis.DEFAULT_MIN_WORD_LENGTH,
                   maxWordLen: Int = MoreLikeThis.DEFAULT_MAX_WORD_LENGTH,
                   maxQueryTerms: Int = MoreLikeThis.DEFAULT_MAX_QUERY_TERMS,
                 ): MoreLikeThisSearchTerm = {
    MoreLikeThisSearchTerm(
      None, value,
      minTermFreq = minTermFreq,
      minDocFreq = minDocFreq,
      maxDocFreq = maxDocFreq,
      boost = boost,
      minWordLen = minWordLen,
      maxWordLen = maxWordLen,
      maxQueryTerms = maxQueryTerms,
    )
  }


  def mlt(
           fv: FieldValue[String],
           minTermFreq: Int = MoreLikeThis.DEFAULT_MIN_TERM_FREQ,
           minDocFreq: Int = MoreLikeThis.DEFAULT_MIN_DOC_FREQ,
           maxDocFreq: Int = MoreLikeThis.DEFAULT_MAX_DOC_FREQ,
           boost: Boolean = MoreLikeThis.DEFAULT_BOOST,
           minWordLen: Int = MoreLikeThis.DEFAULT_MIN_WORD_LENGTH,
           maxWordLen: Int = MoreLikeThis.DEFAULT_MAX_WORD_LENGTH,
           maxQueryTerms: Int = MoreLikeThis.DEFAULT_MAX_QUERY_TERMS,
         ): MoreLikeThisSearchTerm = {
    MoreLikeThisSearchTerm(Some(fv.field), fv.value,
      minTermFreq = minTermFreq,
      minDocFreq = minDocFreq,
      maxDocFreq = maxDocFreq,
      boost = boost,
      minWordLen = minWordLen,
      maxWordLen = maxWordLen,
      maxQueryTerms = maxQueryTerms)
  }

  def spatialBox(field: Field[SpatialPoint], minLatitude: Double, maxLatitude: Double, minLongitude: Double, maxLongitude: Double): SpatialBoxTerm = SpatialBoxTerm(field, minLatitude, maxLatitude, minLongitude, maxLongitude)

  def spatialDistance(field: Field[SpatialPoint], point: SpatialPoint, radius: Double): SpatialDistanceTerm = SpatialDistanceTerm(field, point, radius)

  def spatialPolygon(field: Field[SpatialPoint], polygons: SpatialPolygon*): SpatialPolygonTerm = SpatialPolygonTerm(field, polygons.toList)

  def grouped(minimumNumberShouldMatch: Int, entries: (SearchTerm, Condition)*): GroupedSearchTerm = GroupedSearchTerm(
    minimumNumberShouldMatch = minimumNumberShouldMatch,
    conditionalTerms = entries.toList
  )

  def grouped(entries: (SearchTerm, Condition)*): GroupedSearchTerm = grouped(0, entries: _*)

  def all(terms: SearchTerm*): GroupedSearchTerm = grouped(terms.map(t => t -> Condition.MUST): _*)

  def any(terms: SearchTerm*): GroupedSearchTerm = grouped(minimumNumberShouldMatch = 1, terms.map(t => t -> Condition.SHOULD): _*)

  def none(terms: SearchTerm*): GroupedSearchTerm = grouped(terms.map(t => t -> Condition.MUST_NOT): _*)

  def exists(f: Field[String]): DocValuesExistsSearchTerm = DocValuesExistsSearchTerm(f)

  def boost(term: SearchTerm, boost: Float): BoostedSearchTerm = BoostedSearchTerm(term, boost)

  def drillDown(value: FacetValue, onlyThisLevel: Boolean = false): DrillDownSearchTerm = {
    DrillDownSearchTerm(value.field, value.path, onlyThisLevel)
  }

  implicit class IntFieldExtras(val field: Field[Int]) extends AnyVal {
    def >=(value: Int): SearchTerm = intRange(field, value, Int.MaxValue)

    def >(value: Int): SearchTerm = intRange(field, value + 1, Int.MaxValue)

    def <=(value: Int): SearchTerm = intRange(field, Int.MinValue, value)

    def <(value: Int): SearchTerm = intRange(field, Int.MinValue, value - 1)

    def <=>(value: (Int, Int)): SearchTerm = intRange(field, value._1, value._2)

    def contains(values: Int*): SearchTerm = intSet(field, values)
  }

  implicit class LongFieldExtras(val field: Field[Long]) extends AnyVal {
    def >=(value: Long): SearchTerm = longRange(field, value, Long.MaxValue)

    def >(value: Long): SearchTerm = longRange(field, value + 1, Long.MaxValue)

    def <=(value: Long): SearchTerm = longRange(field, Long.MinValue, value)

    def <(value: Long): SearchTerm = longRange(field, Long.MinValue, value - 1)

    def <=>(value: (Long, Long)): SearchTerm = longRange(field, value._1, value._2)

    def contains(values: Long*): SearchTerm = longSet(field, values)
  }

  private val doublePrecision = 0.0001

  implicit class DoubleFieldExtras(val field: Field[Double]) extends AnyVal {
    def >=(value: Double): SearchTerm = doubleRange(field, value, Double.MaxValue)

    def >(value: Double): SearchTerm = doubleRange(field, value + doublePrecision, Double.MaxValue)

    def <=(value: Double): SearchTerm = doubleRange(field, Double.MinValue, value)

    def <(value: Double): SearchTerm = doubleRange(field, Double.MinValue, value - doublePrecision)

    def <=>(value: (Double, Double)): SearchTerm = doubleRange(field, value._1, value._2)

    def contains(values: Double*): SearchTerm = doubleSet(field, values)
  }

  implicit class SpatialFieldExtras(val field: Field[SpatialPoint]) extends AnyVal {
    def within(length: Double): SpatialPartialDistance = SpatialPartialDistance(field, length)
  }

  case class SpatialPartialDistance(field: Field[SpatialPoint], length: Double) {
    def of(point: SpatialPoint): SpatialDistanceTerm = spatialDistance(field, point, length)
  }

}
