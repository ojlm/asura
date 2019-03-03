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

  def getAllNamespaces(namerdUrl: String)(implicit http: HttpExt): Future[Seq[String]] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(uri = getBaseUrl(namerdUrl), headers = List(ACCEPT_JSON_HEADER)))
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

  def getNamespaceDtabs(namerdUrl: String, namespace: String)(implicit http: HttpExt): Future[Seq[DtabEntry]] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(uri = s"${getBaseUrl(namerdUrl)}/$namespace", headers = List(ACCEPT_JSON_HEADER)))
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

  def createNamespaceDtabs(namerdUrl: String, namespace: String, dtabs: Seq[DtabEntry])(implicit http: HttpExt): Future[String] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(
      method = HttpMethods.POST,
      uri = s"${getBaseUrl(namerdUrl)}/$namespace",
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

  def updateNamespaceDtabs(namerdUrl: String, namespace: String, dtabs: Seq[DtabEntry])(implicit http: HttpExt): Future[String] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(
      method = HttpMethods.PUT,
      uri = s"${getBaseUrl(namerdUrl)}/$namespace",
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

  def deleteNamespaceDtabs(namerdUrl: String, namespace: String)(implicit http: HttpExt): Future[String] = {
    import NamerdConfig._
    http.singleRequest(HttpRequest(
      method = HttpMethods.DELETE,
      uri = s"${getBaseUrl(namerdUrl)}/$namespace"
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

  def getBaseUrl(namerdUrl: String) = s"${namerdUrl}/api/1/dtabs"
}
