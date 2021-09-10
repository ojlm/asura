package asura.ui.cli.store.lucene.query

import org.apache.lucene.facet.FacetsCollector
import org.apache.lucene.index.LeafReaderContext
import org.apache.lucene.search._

case class Collectors(
                       topFieldCollector: TopFieldCollector,
                       facetsCollector: FacetsCollector,
                     ) extends Collector {

  override def getLeafCollector(context: LeafReaderContext): LeafCollector = {
    val topFieldLeaf = try {
      Some(topFieldCollector.getLeafCollector(context))
    } catch {
      case _: CollectionTerminatedException => None
    }
    val facetsLeaf = try {
      Some(facetsCollector.getLeafCollector(context))
    } catch {
      case _: CollectionTerminatedException => None
    }
    val leafCollectors = List(topFieldLeaf, facetsLeaf).flatten
    leafCollectors match {
      case Nil => throw new CollectionTerminatedException()
      case leafCollectors :: Nil => leafCollectors
      case _ => new CollectorsLeafCollector(leafCollectors)
    }
  }

  override def scoreMode(): ScoreMode = {
    val sm1 = topFieldCollector.scoreMode()
    val sm2 = facetsCollector.scoreMode()
    if (sm1 == ScoreMode.COMPLETE || sm2 == ScoreMode.COMPLETE) {
      ScoreMode.COMPLETE
    } else if (sm1 == ScoreMode.TOP_SCORES || sm2 == ScoreMode.TOP_SCORES)
      ScoreMode.TOP_SCORES
    else {
      sm1
    }
  }

}
