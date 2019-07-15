package asura.kafka.consumer

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.common.serialization.StringDeserializer

object TestConsumer extends StrictLogging {

  def main(args: Array[String]): Unit = {

    logger.info("Start consumer")

    implicit val system = ActorSystem("consumer")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withGroupId("test-group1")

    val done = Consumer
      .plainSource(consumerSettings, Subscriptions.topics("test-topic"))
      .runWith(Sink.foreach(record =>
        logger.info(s"topic:${record.topic()}, partition:${record.partition()}, offset:${record.offset()}, key:${record.key()}, value: ${record.value()}"))
      )
    done onComplete {
      case scala.util.Success(_) => logger.info("Done"); system.terminate()
      case scala.util.Failure(err) => logger.error(err.toString); system.terminate()
    }
  }
}
