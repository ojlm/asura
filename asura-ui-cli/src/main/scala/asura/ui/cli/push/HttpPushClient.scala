package asura.ui.cli.push

import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.nio.charset.StandardCharsets

import asura.common.util.{HttpUtils, JsonUtils}
import asura.ui.cli.CliSystem
import com.typesafe.scalalogging.Logger

case class HttpPushClient(options: PushOptions) extends SimpleSendPushClient {

  val logger = Logger(getClass)

  override def send(data: PushDataMessage): Unit = {
    val builder = HttpRequest.newBuilder(URI.create(options.pushUrl))
      .POST(BodyPublishers.ofString(JsonUtils.stringify(data), StandardCharsets.UTF_8))
    builder.header("Content-Type", "application/json")
    HttpUtils.sendAsync(builder.build()).map(response => {
      if (response.statusCode() != 200) {
        logger.warn(s"type(${data.`type`}) send error: ${response.body()}")
      }
    })(CliSystem.ec)
  }

}
