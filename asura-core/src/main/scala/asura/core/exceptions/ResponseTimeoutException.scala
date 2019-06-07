package asura.core.exceptions

import akka.http.scaladsl.model.HttpRequest

final case class ResponseTimeoutException(request: HttpRequest, message: String) extends RuntimeException(message)
