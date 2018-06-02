package asura.core.es

import asura.core.es.model.FieldKeys
import com.sksamuel.elastic4s.http.search.SearchResponse

object EsResponse {

  def toApiData(res: SearchResponse): Map[String, Any] = {
    val hits = res.hits
    Map("total" -> hits.total, "list" -> hits.hits.map(hit => {
      hit.sourceAsMap + (FieldKeys.FIELD__ID -> hit.id)
    }))
  }

  def toSingleApiData(res: SearchResponse): Map[String, Any] = {
    val hits = res.hits
    if (hits.nonEmpty) {
      val hit = hits.hits(0)
      hit.sourceAsMap + (FieldKeys.FIELD__ID -> hit.id)
    } else {
      Map.empty
    }
  }
}
