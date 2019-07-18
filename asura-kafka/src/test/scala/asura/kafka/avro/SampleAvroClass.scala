package asura.kafka.avro

import org.apache.avro.Schema
import org.apache.avro.specific.{AvroGenerated, SpecificRecordBase}

@AvroGenerated
case class SampleAvroClass(key: String, value: String) extends SpecificRecordBase {
  override def getSchema: Schema = null

  override def get(field: Int): AnyRef = null

  override def put(field: Int, value: Any): Unit = {}
}
