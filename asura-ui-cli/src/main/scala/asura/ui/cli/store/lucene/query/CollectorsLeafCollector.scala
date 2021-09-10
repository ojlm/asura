package asura.ui.cli.store.lucene.query

import org.apache.lucene.search.{LeafCollector, Scorable}

class CollectorsLeafCollector(leafCollectors: List[LeafCollector]) extends LeafCollector {

  override def setScorer(scorer: Scorable): Unit = {
    leafCollectors.foreach(_.setScorer(scorer))
  }

  override def collect(doc: Int): Unit = {
    leafCollectors.foreach(_.collect(doc))
  }

}
