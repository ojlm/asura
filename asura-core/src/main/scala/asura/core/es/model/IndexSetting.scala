package asura.core.es.model

import com.sksamuel.elastic4s.mappings.MappingDefinition

trait IndexSetting {

  val Index: String
  val mappings: MappingDefinition
  val shards: Int = 5
  val replicas: Int = 1
}
