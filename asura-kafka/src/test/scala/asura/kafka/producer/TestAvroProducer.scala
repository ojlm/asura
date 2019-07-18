package asura.kafka.producer

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import asura.kafka.avro.SampleAvroClass
import com.typesafe.scalalogging.StrictLogging
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializerConfig, KafkaAvroSerializer}
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization._

import scala.collection.JavaConverters._

// https://doc.akka.io/docs/alpakka-kafka/current/serialization.html
object TestAvroProducer extends StrictLogging {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("producer")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val schemaRegistryUrl = ""
    val bootstrapServers = ""
    val topic = ""

    val kafkaAvroSerDeConfig = Map[String, Any](
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl,
      KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG -> true.toString
    )
    val producerSettings: ProducerSettings[String, SpecificRecord] = {
      val kafkaAvroSerializer = new KafkaAvroSerializer()
      kafkaAvroSerializer.configure(kafkaAvroSerDeConfig.asJava, false)
      val serializer = kafkaAvroSerializer.asInstanceOf[Serializer[SpecificRecord]]

      ProducerSettings(system, new StringSerializer, serializer)
        .withBootstrapServers(bootstrapServers)
    }

    val samples = (1 to 3).map(i => SampleAvroClass(s"key_$i", s"name_$i"))
    val done = Source(samples)
      .map(n => new ProducerRecord[String, SpecificRecord](topic, n.key, n))
      .runWith(Producer.plainSink(producerSettings))

    done onComplete {
      case scala.util.Success(_) => logger.info("Done"); system.terminate()
      case scala.util.Failure(err) => logger.error(err.toString); system.terminate()
    }
  }
}
