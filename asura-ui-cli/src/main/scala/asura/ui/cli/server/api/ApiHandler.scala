package asura.ui.cli.server.api

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

import scala.concurrent.Future

import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.{JsonUtils, StringUtils}
import asura.ui.cli.CliSystem
import asura.ui.cli.server.api.ApiHandler.FUTURE_API_NOT_FOUND
import asura.ui.cli.server.ide.local.LocalIde
import asura.ui.ide.Ide
import com.typesafe.scalalogging.Logger
import karate.io.netty.handler.codec.http.{FullHttpRequest, HttpMethod, QueryStringDecoder}

trait ApiHandler {

  def currentUser(implicit uri: QueryStringDecoder, req: FullHttpRequest): String = {
    // todo: extract from the uri or headers
    LocalIde.DEFAULT_USERNAME
  }

  def ide(implicit uri: QueryStringDecoder): Ide = {
    val list = uri.parameters().get("ide")
    if (list != null && !list.isEmpty) {
      list.get(0) match {
        case "local" => CliSystem.localIde
        case "remote" => CliSystem.remoteIde
        case v => throw new RuntimeException(s"Unsupported ide $v")
      }
    } else {
      if (CliSystem.localIde != null) {
        CliSystem.localIde
      } else if (CliSystem.remoteIde != null) {
        CliSystem.remoteIde
      } else {
        throw new RuntimeException(s"No available ide")
      }
    }
  }

  def extractToString(implicit req: FullHttpRequest): String = {
    val body = req.content().toString(StandardCharsets.UTF_8)
    if (StringUtils.isEmpty(body)) {
      throw new RuntimeException("Request body is empty")
    }
    body
  }

  def extractTo[T <: AnyRef](c: Class[T])(implicit req: FullHttpRequest): T = {
    JsonUtils.parse(extractToString(req), c)
  }

  def handle(path: Seq[String], uri: QueryStringDecoder, req: FullHttpRequest): Future[ApiRes] = {
    implicit val ec = CliSystem.ec
    val res = req.method() match {
      case HttpMethod.GET => get(path)(uri, req)
      case HttpMethod.POST => post(path)(uri, req)
      case HttpMethod.PUT => put(path)(uri, req)
      case HttpMethod.DELETE => delete(path)(uri, req)
      case _ => FUTURE_API_NOT_FOUND
    }
    res.map(data => ApiRes(data = data))
  }

  def get(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    notFound(uri)
  }

  def post(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    notFound(uri)
  }

  def put(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    notFound(uri)
  }

  def delete(path: Seq[String])(implicit uri: QueryStringDecoder, req: FullHttpRequest): Future[_] = {
    notFound(uri)
  }

  private def notFound(uri: QueryStringDecoder): Future[_] = {
    Future.failed(new RuntimeException(s"${uri.path()} not found"))
  }

}

object ApiHandler {

  val logger = Logger("ApiHandler")
  val API_NOT_FOUND = ApiResError("api not found")
  val FUTURE_API_NOT_FOUND = Future.successful(API_NOT_FOUND)
  lazy val HANDLERS = {
    val handlers = new ConcurrentHashMap[String, ApiHandler]()
    handlers.put("user", new UserApi())
    handlers.put("workspace", new WorkspaceApi())
    handlers.put("project", new ProjectApi())
    handlers.put("file", new FileApi())
    handlers.put("tree", new TreeApi())
    handlers.put("blob", new BlobApi())
    handlers.put("devices", new DevicesApi())
    handlers.put("web", new WebApi())
    handlers.put("run", new RunApi())
    handlers
  }

  def handle(req: FullHttpRequest): Future[ApiRes] = {
    implicit val ec = CliSystem.ec
    try {
      val uri = new QueryStringDecoder(req.uri())
      val paths = uri.path().split("/")
      paths match { // ["", "api", ...]
        case Array(_, _, name, tail@_*) =>
          val handler = HANDLERS.get(name)
          if (handler != null) {
            handler.handle(tail, uri, req).recover {
              case t: Throwable =>
                logger.warn(s"${req.method().name()} ${uri.uri()}", t)
                ApiResError(t.getMessage)
            }
          } else {
            FUTURE_API_NOT_FOUND
          }
        case _ => FUTURE_API_NOT_FOUND
      }
    } catch {
      case t: Throwable =>
        logger.warn("api error", t)
        Future.successful(ApiResError(t.getMessage))
    }
  }

}
