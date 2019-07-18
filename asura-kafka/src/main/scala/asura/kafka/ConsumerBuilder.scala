package asura.kafka

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Source
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer, KafkaAvroDeserializerConfig}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}

import scala.collection.JavaConverters._

object ConsumerBuilder {

  def buildAvroSource[V](
                          brokerUrl: String,
                          schemaRegisterUrl: String,
                          group: String,
                          topics: Set[String],
                          resetType: String = "latest",
                        )(implicit system: ActorSystem): Source[ConsumerRecord[String, V], Control] = {

    val kafkaAvroSerDeConfig = Map[String, Any](
      AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> schemaRegisterUrl,
      KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG -> true.toString
    )
    val consumerSettings: ConsumerSettings[String, V] = {
      val kafkaAvroDeserializer = new KafkaAvroDeserializer()
      kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, false)
      val deserializer = kafkaAvroDeserializer.asInstanceOf[Deserializer[V]]

      ConsumerSettings(system, new StringDeserializer, deserializer)
        .withBootstrapServers(brokerUrl)
        .withGroupId(group)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, resetType)
    }
    Consumer.plainSource(consumerSettings, Subscriptions.topics(topics))
  }
}
