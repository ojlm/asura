package asura.core.es.model

import asura.core.es.EsConfig
import com.sksamuel.elastic4s.mappings.{FieldDefinition, MappingDefinition}

trait IndexSetting {

  val Index: String
  val mappings: MappingDefinition
  val shards: Int = 5
  val replicas: Int = 1

  object Es6MappingDefinition {
    def apply(fields: Seq[FieldDefinition]): MappingDefinition = {
      MappingDefinition(`type` = EsConfig.DefaultType, fields = fields)
    }
  }

}
