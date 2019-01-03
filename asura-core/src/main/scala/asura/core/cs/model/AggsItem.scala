package asura.core.cs.model

import asura.core.cs.model.AggsItem.Metrics
import asura.core.es.service.BaseAggregationService

case class AggsItem(
                     var `type`: String,
                     var id: String,
                     var count: Long,
                     var sub: Seq[AggsItem] = null,
                     var metrics: Metrics = null,
                     var summary: String = null,
                     var description: String = null,
                   ) {

  def evaluateBucketToMetrics(bucket: Map[String, Any], resolution: Double = 1D): AggsItem = {
    if (null != bucket && bucket.get(BaseAggregationService.aggsPercentilesName).nonEmpty) {
      val percentiles = bucket.get(BaseAggregationService.aggsPercentilesName)
        .get.asInstanceOf[Map[String, Map[String, Double]]].getOrElse("values", Map.empty)
      val min = bucket.get(BaseAggregationService.aggsMin).getOrElse(Map.empty).asInstanceOf[Map[String, Double]]
      val avg = bucket.get(BaseAggregationService.aggsAvg).getOrElse(Map.empty).asInstanceOf[Map[String, Double]]
      val max = bucket.get(BaseAggregationService.aggsMax).getOrElse(Map.empty).asInstanceOf[Map[String, Double]]
      metrics = Metrics(
        p25 = Math.round(percentiles.get("25.0").getOrElse(0D) * resolution).toInt,
        p50 = Math.round(percentiles.get("50.0").getOrElse(0D) * resolution).toInt,
        p75 = Math.round(percentiles.get("75.0").getOrElse(0D) * resolution).toInt,
        p90 = Math.round(percentiles.get("90.0").getOrElse(0D) * resolution).toInt,
        p95 = Math.round(percentiles.get("95.0").getOrElse(0D) * resolution).toInt,
        p99 = Math.round(percentiles.get("99.0").getOrElse(0D) * resolution).toInt,
        p999 = Math.round(percentiles.get("99.9").getOrElse(0D) * resolution).toInt,
        min = Math.round(min.get("value").getOrElse(0D) * resolution).toInt,
        avg = Math.round(avg.get("value").getOrElse(0D) * resolution).toInt,
        max = Math.round(max.get("value").getOrElse(0D) * resolution).toInt,
      )
    }
    this
  }
}

object AggsItem {

  case class Metrics(
                      p25: Int,
                      p50: Int,
                      p75: Int,
                      p90: Int,
                      p95: Int,
                      p99: Int,
                      p999: Int,
                      min: Int,
                      avg: Int,
                      max: Int,
                    )

}
