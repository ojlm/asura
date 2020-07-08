package asura.kafka.consumer

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink}
import asura.kafka.avro.SampleAvroClass
import com.typesafe.scalalogging.StrictLogging
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer, KafkaAvroDeserializerConfig}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization._

import scala.collection.JavaConverters._

object TestAvroConsumer extends StrictLogging {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("consumer")
    implicit val materializer = Materializer(system)
    implicit val ec = system.dispatcher

    val schemaRegistryUrl = ""
    val bootstrapServers = ""
    val topic = ""
    val group = ""

    val kafkaAvroSerDeConfig = Map[String, Any](
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegistryUrl,
      KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG -> true.toString
    )
    val consumerSettings: ConsumerSettings[String, SampleAvroClass] = {
      val kafkaAvroDeserializer = new KafkaAvroDeserializer()
      kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, false)
      val deserializer = kafkaAvroDeserializer.asInstanceOf[Deserializer[SampleAvroClass]]

      ConsumerSettings(system, new StringDeserializer, deserializer)
        .withBootstrapServers(bootstrapServers)
        .withGroupId(group)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    }

    val samples = (1 to 3)
    val (control, result) = Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .take(samples.size.toLong)
      .map(_.value())
      .toMat(Sink.seq)(Keep.both)
      .run()

    control.shutdown()
    result.map(records => records.foreach(record => logger.info(s"${record}")))
  }
}
