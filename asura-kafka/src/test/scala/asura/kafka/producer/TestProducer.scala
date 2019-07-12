package asura.kafka.producer

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

object TestProducer extends StrictLogging {

  def main(args: Array[String]): Unit = {

    logger.info("Start producer")

    implicit val system = ActorSystem("producer")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher

    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    val done: Future[Done] =
      Source(1 to 100)
        .map(value => new ProducerRecord[String, String]("test-topic", s"msg ${value}"))
        .runWith(Producer.plainSink(producerSettings))

    done onComplete {
      case scala.util.Success(_) => logger.info("Done"); system.terminate()
      case scala.util.Failure(err) => logger.error(err.toString); system.terminate()
    }
  }
}
