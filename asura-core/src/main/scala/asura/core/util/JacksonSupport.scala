package asura.core.util

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.Materializer
import asura.common.util.JsonUtils
import com.sksamuel.elastic4s.Indexable

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object JacksonSupport extends JsonUtils {

  val mapper = JsonUtils.mapper

  implicit def JacksonMarshaller: ToEntityMarshaller[AnyRef] = {
    Marshaller.withFixedContentType(`application/json`) { obj =>
      HttpEntity(`application/json`, mapper.writeValueAsString(obj).getBytes("UTF-8"))
    }
  }

  implicit def jacksonUnmarshaller[T <: AnyRef](implicit c: ClassTag[T]): FromRequestUnmarshaller[T] = {
    new FromRequestUnmarshaller[T] {
      override def apply(request: HttpRequest)(implicit ec: ExecutionContext, materializer: Materializer): Future[T] = {
        request.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8")).map { str =>
          mapper.readValue(str, c.runtimeClass).asInstanceOf[T]
        }
      }
    }
  }

  implicit def jacksonJsonIndexable[T]: Indexable[T] = {
    new Indexable[T] {
      override def json(t: T): String = mapper.writeValueAsString(t)
    }
  }
}
