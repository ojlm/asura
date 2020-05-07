package asura.core.es

import com.sksamuel.elastic4s.requests.mappings.Analysis

object EsConfig {

  /** this can be override by configuration file */
  var IndexPrefix = "asura-"
  val MaxCount = 1000

  val DateFormat = "yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
  var IK_ANALYZER = Analysis(analyzer = Option("ik_smart"), searchAnalyzer = Option("ik_smart"))
}
