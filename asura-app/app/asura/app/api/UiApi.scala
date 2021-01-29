package asura.app.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws._
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import asura.app.AppErrorMessages
import asura.common.model.ApiRes
import asura.common.util.StringUtils
import asura.core.actor.flow.WebSocketMessageHandler
import asura.core.es.model.Permissions.Functions
import asura.core.security.PermissionAuthProvider
import asura.ui.actor.WebControllerActor
import asura.ui.driver.{DriverCommand, Drivers, UiDriverProvider}
import asura.ui.model.MobileDriverInfo
import javax.inject.{Inject, Singleton}
import org.pac4j.http.client.direct.HeaderClient
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration
import play.api.http.HttpEntity
import play.api.mvc.{ResponseHeader, Result, WebSocket}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UiApi @Inject()(
                       implicit val system: ActorSystem,
                       implicit val exec: ExecutionContext,
                       implicit val mat: Materializer,
                       val configuration: Configuration,
                       val controllerComponents: SecurityComponents,
                       val client: HeaderClient,
                       val permissionAuthProvider: PermissionAuthProvider,
                       val uiDriverProvider: UiDriverProvider,
                     ) extends BaseApi {

  // this instance
  val websockifyRfbWsUrl = configuration.getOptional[String]("asura.ui.proxy.url").getOrElse(StringUtils.EMPTY)

  def register(driverType: String) = Action(parse.byteString).async { implicit req =>
    val driverInfo = driverType match {
      case Drivers.ANDROID =>
        req.bodyAs(classOf[MobileDriverInfo])
      case _ => null
    }
    if (driverInfo != null && driverInfo.isValid()) {
      uiDriverProvider.register(driverType, driverInfo)
      Future.successful(ApiRes()).toOkResult
    } else {
      toI18nFutureErrorResult(AppErrorMessages.error_InvalidRequestParameters)
    }
  }

  def driverList(group: String, project: String, driverType: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      uiDriverProvider.getDrivers(driverType).toOkResult
    }
  }

  // proxy to 'connect' routes
  def proxyToConnect(
                      group: String,
                      project: String,
                      id: String,
                      host: String,
                      port: String,
                      token: String
                    ) = WebSocket.acceptOrResult[String, String] { implicit req =>
    checkWsPermission(group, project, Functions.PROJECT_COMPONENT_EXEC) { _ =>
      Right {
        val url = s"ws://$host:$port/api/ui/driver/connect/$group/$project/$id?token=$token"
        Flow[String]
          .map(msg => TextMessage.Strict(msg))
          .via(webSocketFlow(url))
          .map(msg => msg.asTextMessage.getStrictText)
      }
    }
  }

  // proxy to 'rfb' routes
  def proxyToRfb(
                  group: String,
                  project: String,
                  host: String,
                  port: String,
                  token: String
                ) = WebSocket.acceptOrResult[ByteString, ByteString] { implicit req =>
    checkWsPermission(group, project, Functions.PROJECT_COMPONENT_EXEC) { _ =>
      Right {
        val url = s"ws://$host:$port/api/ui/rfb/$group/$project?token=$token"
        Flow[ByteString]
          .map(msg => BinaryMessage(msg))
          .via(webSocketFlow(url))
          .mapAsync(10)(msgToBytes)
      }
    }
  }

  def connect(group: String, project: String, id: String) = WebSocket.acceptOrResult[String, String] { implicit req =>
    checkWsPermission(group, project, Functions.PROJECT_COMPONENT_VIEW) { user =>
      Right {
        val controller = system.actorOf(WebControllerActor.props(group, project, id, user))
        WebSocketMessageHandler.stringToActorEventFlow(controller, classOf[DriverCommand])
      }
    }
  }

  def rfb(group: String, project: String) = WebSocket.acceptOrResult[ByteString, ByteString] { implicit req =>
    checkWsPermission(group, project, Functions.PROJECT_COMPONENT_EXEC) { _ =>
      if (StringUtils.isNotEmpty(websockifyRfbWsUrl)) {
        Right {
          Flow[ByteString]
            .map(msg => BinaryMessage(msg))
            .via(webSocketFlow(websockifyRfbWsUrl))
            .mapAsync(10)(msgToBytes)
        }
      } else {
        Left(Result(header = ResponseHeader(FORBIDDEN), body = HttpEntity.Strict(ByteString("Empty proxy url"), None)))
      }
    }
  }

  // convert from akka 'Message' to play 'Message'
  private def msgToBytes(msg: Message): Future[ByteString] = {
    msg match {
      case TextMessage.Strict(data) =>
        Future.successful(ByteString(data.getBytes("UTF-8")))
      case TextMessage.Streamed(stream) =>
        stream.fold("")(_ + _).map(_.getBytes("UTF-8"))
          .runWith(Sink.head)
          .map(bytes => ByteString(bytes))
      case BinaryMessage.Strict(data) =>
        Future.successful(data)
      case BinaryMessage.Streamed(stream) =>
        stream.fold(ByteString.empty)(_ ++ _).runWith(Sink.head)
    }
  }

  private def webSocketFlow(url: String): Flow[Message, Message, Future[WebSocketUpgradeResponse]] =
    Http().webSocketClientFlow(WebSocketRequest(url))
}
