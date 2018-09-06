package asura.core.es

import com.sksamuel.elastic4s.mappings.Analysis


object EsConfig {

  var IndexPrefix = "asura-"
  /** no use, type was deprecated since 6.0.0. */
  val DefaultType = "default"
  val MaxCount = 1000

  val DateFormat = "yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
  var IK_ANALYZER = Analysis(analyzer = Option("ik_smart"), searchAnalyzer = Option("ik_smart"))
}
