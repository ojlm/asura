package asura.core.es

import asura.core.es.model.FieldKeys
import com.sksamuel.elastic4s.requests.searches.SearchResponse

import scala.collection.mutable

object EsResponse {

  def toApiData(res: SearchResponse, hasId: Boolean = true): Map[String, Any] = {
    val hits = res.hits
    Map("total" -> hits.total.value, "list" -> hits.hits.map(hit => {
      if (hasId) {
        hit.sourceAsMap + (FieldKeys.FIELD__ID -> hit.id)
      } else {
        hit.sourceAsMap
      }
    }))
  }

  def toSingleApiData(res: SearchResponse, hasId: Boolean = true): Map[String, Any] = {
    val hits = res.hits
    if (hits.nonEmpty) {
      val hit = hits.hits(0)
      if (hasId) {
        hit.sourceAsMap + (FieldKeys.FIELD__ID -> hit.id)
      } else {
        hit.sourceAsMap
      }
    } else {
      Map.empty
    }
  }

  def toSeq(res: SearchResponse, hasId: Boolean = true): Seq[Map[String, Any]] = {
    res.hits.hits.toIndexedSeq.map(hit => {
      if (hasId) {
        hit.sourceAsMap + (FieldKeys.FIELD__ID -> hit.id)
      } else {
        hit.sourceAsMap
      }
    })
  }

  def toIdMap(res: SearchResponse): Map[String, Map[String, Any]] = {
    val map = mutable.HashMap[String, Map[String, Any]]()
    res.hits.hits.toIndexedSeq.map(hit => map(hit.id) = hit.sourceAsMap)
    map.toMap
  }
}
