package asura.namerd.api.v1

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import asura.common.exceptions.IllegalRequestException
import asura.common.util.{FutureUtils, JsonUtils}
import asura.namerd.{DtabEntry, NamerdConfig}
import com.fasterxml.jackson.core.`type`.TypeReference

import scala.concurrent.Future

object NamerdV1Api extends NamerdV1Api {

  def getAllNamespaces()(implicit http: HttpExt): Future[Seq[String]] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(uri = API_V1_NAMESPACE, headers = List(ACCEPT_JSON_HEADER)))
      .flatMap(res =>
        if (res.status.isSuccess()) {
          Unmarshal(res.entity).to[String].map(str =>
            JsonUtils.parse(str, new TypeReference[Seq[String]] {})
          )
        } else {
          FutureUtils.requestFail(res.status.reason())
        }
      )
  }

  def getNamespaceDtabs(namespace: String)(implicit http: HttpExt): Future[Seq[DtabEntry]] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(uri = s"$API_V1_NAMESPACE/$namespace", headers = List(ACCEPT_JSON_HEADER)))
      .flatMap(res =>
        if (res.status.isSuccess()) {
          Unmarshal(res.entity).to[String].map(str =>
            JsonUtils.parse(str, new TypeReference[Seq[DtabEntry]] {})
          )
        } else {
          FutureUtils.requestFail(res.status.reason())
        }
      )
  }

  def createNamespaceDtabs(namespace: String, dtabs: Seq[DtabEntry])(implicit http: HttpExt): Future[String] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = s"$API_V1_NAMESPACE/$namespace",
      entity = HttpEntity(ContentTypes.`application/json`, JsonUtils.stringify(dtabs))
    )).map(res =>
      res.status.intValue() match {
        case 204 =>
          res.discardEntityBytes()
          "Created";
        case 400 =>
          res.discardEntityBytes()
          throw IllegalRequestException("Dtab is malformed")
        case 409 =>
          res.discardEntityBytes()
          throw IllegalRequestException("Dtab namespace already exists")
        case _ =>
          res.discardEntityBytes()
          throw IllegalRequestException(s"Unknown code: ${res.status.value}")
      }
    )
  }

  def updateNamespaceDtabs(namespace: String, dtabs: Seq[DtabEntry])(implicit http: HttpExt): Future[String] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(
      method = HttpMethods.PUT,
      uri = s"$API_V1_NAMESPACE/$namespace",
      entity = HttpEntity(ContentTypes.`application/json`, JsonUtils.stringify(dtabs))
    )).map(res =>
      res.status.intValue() match {
        case 204 =>
          res.discardEntityBytes()
          "Updated"
        case 400 =>
          res.discardEntityBytes()
          throw IllegalRequestException("Dtab is malformed")
        case 404 =>
          res.discardEntityBytes()
          throw IllegalRequestException("Dtab namespace does not exist")
        case 412 =>
          res.discardEntityBytes()
          throw IllegalRequestException("If-Match header is provided and does not match the current dtab version")
        case _ =>
          res.discardEntityBytes()
          throw IllegalRequestException(s"Unknown code: ${res.status.value}")
      }
    )
  }

  def deleteNamespaceDtabs(namespace: String)(implicit http: HttpExt): Future[String] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(
      method = HttpMethods.DELETE,
      uri = s"$API_V1_NAMESPACE/$namespace"
    )).map(res =>
      res.status.intValue() match {
        case 204 =>
          res.discardEntityBytes()
          "Deleted"
        case 404 =>
          res.discardEntityBytes()
          throw IllegalRequestException("Dtab namespace does not exist")
        case _ =>
          res.discardEntityBytes()
          throw IllegalRequestException(s"Unknown code: ${res.status.value}")
      }
    )
  }
}

trait NamerdV1Api {

  val ACCEPT_JSON_HEADER = HttpHeader.parse("Accept", "application/json") match {
    case Ok(header: HttpHeader, _) =>
      header
    case Error(_) =>
      RawHeader("Accept", "application/json")
  }

  val API_V1_NAMESPACE = s"${NamerdConfig.url}/api/1/dtabs"
}
