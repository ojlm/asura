package asura.ui.cli.store.lucene.query

import java.io.StringReader

import asura.ui.cli.store.lucene.Lucene
import asura.ui.cli.store.lucene.field.{FacetField, Field, SpatialPoint}
import org.apache.lucene.document.{DoublePoint, IntPoint, LatLonPoint, LongPoint}
import org.apache.lucene.facet.DrillDownQuery
import org.apache.lucene.geo.Polygon
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search._
import org.apache.lucene.util.automaton.RegExp

trait SearchTerm {
  def toLucene(lucene: Lucene): Query
}

object SearchTerm {

  object MatchAll extends SearchTerm {
    private lazy val instance = new MatchAllDocsQuery()

    override def toLucene(lucene: Lucene): Query = instance
  }

  case class ParsableSearchTerm(field: Option[Field[String]], value: String, allowLeadingWildcard: Boolean) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      val parser = new QueryParser(field.getOrElse(lucene.fullText).filterName, lucene.analyzer)
      parser.setAllowLeadingWildcard(allowLeadingWildcard)
      parser.parse(value)
    }
  }

  case class PhraseSearchTerm(field: Option[Field[String]], value: String, slop: Int = 0) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      new PhraseQuery(slop, field.getOrElse(lucene.fullText).filterName, value.split(' ').map(_.toLowerCase): _*)
    }
  }

  case class TermSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      new TermQuery(new Term(field.getOrElse(lucene.fullText).filterName, value))
    }
  }

  case class DrillDownSearchTerm(facet: FacetField, path: Seq[String], onlyThisLevel: Boolean) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      val name = lucene.facetsConfig.getDimConfig(facet.name).indexFieldName
      val exactPath = if (onlyThisLevel) {
        path.toList ::: List("$ROOT$")
      } else {
        path
      }
      new TermQuery(DrillDownQuery.term(name, facet.name, exactPath: _*))
    }
  }

  case class MoreLikeThisSearchTerm(
                                     field: Option[Field[String]],
                                     value: String,
                                     minTermFreq: Int,
                                     minDocFreq: Int,
                                     maxDocFreq: Int,
                                     boost: Boolean,
                                     minWordLen: Int,
                                     maxWordLen: Int,
                                     maxQueryTerms: Int,
                                   ) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      val fieldName = field.getOrElse(lucene.fullText).filterName
      val mlt = lucene.moreLikeThis
      mlt.setFieldNames(Array(fieldName))
      mlt.setMinTermFreq(minTermFreq)
      mlt.setMinDocFreq(minDocFreq)
      mlt.setMaxDocFreq(maxDocFreq)
      mlt.setBoost(boost)
      mlt.setMinWordLen(minWordLen)
      mlt.setMaxWordLen(maxWordLen)
      mlt.setMaxQueryTerms(maxQueryTerms)
      mlt.like(fieldName, new StringReader(value))
    }
  }

  case class ExactBooleanSearchTerm(field: Field[Boolean], value: Boolean) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      IntPoint.newExactQuery(field.filterName, if (value) 1 else 0)
    }
  }

  case class ExactIntSearchTerm(field: Field[Int], value: Int) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      IntPoint.newExactQuery(field.filterName, value)
    }
  }

  case class ExactLongSearchTerm(field: Field[Long], value: Long) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      LongPoint.newExactQuery(field.filterName, value)
    }
  }

  case class ExactDoubleSearchTerm(field: Field[Double], value: Double) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      DoublePoint.newExactQuery(field.filterName, value)
    }
  }

  case class RangeIntSearchTerm(field: Field[Int], start: Int, end: Int) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      IntPoint.newRangeQuery(field.filterName, start, end)
    }
  }

  case class RangeLongSearchTerm(field: Field[Long], start: Long, end: Long) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      LongPoint.newRangeQuery(field.filterName, start, end)
    }
  }

  case class RangeDoubleSearchTerm(field: Field[Double], start: Double, end: Double) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      DoublePoint.newRangeQuery(field.filterName, start, end)
    }
  }

  case class SetIntSearchTerm(field: Field[Int], set: Seq[Int]) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      IntPoint.newSetQuery(field.filterName, set: _*)
    }
  }

  case class SetLongSearchTerm(field: Field[Long], set: Seq[Long]) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = LongPoint.newSetQuery(field.filterName, set: _*)
  }

  case class SetDoubleSearchTerm(field: Field[Double], set: Seq[Double]) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = DoublePoint.newSetQuery(field.filterName, set: _*)
  }

  case class RegexpSearchTerm(field: Option[Field[String]], value: String, flags: Int = RegExp.ALL) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      new RegexpQuery(new Term(field.getOrElse(lucene.fullText).filterName, value), flags)
    }
  }

  case class WildcardSearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      new WildcardQuery(new Term(field.getOrElse(lucene.fullText).filterName, value))
    }
  }

  case class FuzzySearchTerm(field: Option[Field[String]], value: String) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      new FuzzyQuery(new Term(field.getOrElse(lucene.fullText).filterName, value))
    }
  }

  case class SpatialBoxTerm(field: Field[SpatialPoint], minLatitude: Double, maxLatitude: Double, minLongitude: Double, maxLongitude: Double) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      LatLonPoint.newBoxQuery(field.filterName, minLatitude, maxLatitude, minLongitude, maxLongitude)
    }
  }

  case class SpatialDistanceTerm(field: Field[SpatialPoint], point: SpatialPoint, radiusMeters: Double) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = LatLonPoint.newDistanceQuery(field.filterName, point.latitude, point.longitude, radiusMeters)
  }

  case class SpatialPolygon(points: List[SpatialPoint], holes: List[SpatialPolygon] = Nil) {
    def toLucene: Polygon = {
      new Polygon(points.map(_.latitude).toArray, points.map(_.longitude).toArray, holes.map(_.toLucene): _*)
    }
  }

  case class SpatialPolygonTerm(field: Field[SpatialPoint], polygons: List[SpatialPolygon]) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = LatLonPoint.newPolygonQuery(field.filterName, polygons.map(_.toLucene): _*)
  }

  case class GroupedSearchTerm(
                                minimumNumberShouldMatch: Int,
                                conditionalTerms: Seq[(SearchTerm, Condition)],
                              ) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      val builder = new BooleanQuery.Builder()
      builder.setMinimumNumberShouldMatch(minimumNumberShouldMatch)
      if (conditionalTerms.forall(_._2 == Condition.MUST_NOT)) {
        builder.add(MatchAll.toLucene(lucene), Condition.MUST.occur)
      }
      conditionalTerms.foreach {
        case (st, c) => builder.add(st.toLucene(lucene), c.occur)
      }
      builder.build()
    }
  }

  case class BoostedSearchTerm(term: SearchTerm, boost: Float) extends SearchTerm {
    override def toLucene(lucene: Lucene): Query = {
      new BoostQuery(term.toLucene(lucene), boost)
    }
  }

}
