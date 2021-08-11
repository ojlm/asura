package asura.ui.cli.server

import java.io.{File, InputStream, UnsupportedEncodingException}
import java.net.{URL, URLDecoder}
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.{JsonUtils, LogUtils}
import asura.ui.cli.CliSystem
import asura.ui.cli.server.HttpPageHandler._
import asura.ui.cli.task.TaskInfo
import com.typesafe.scalalogging.Logger
import karate.io.netty.buffer.Unpooled
import karate.io.netty.channel._
import karate.io.netty.handler.codec.http.HttpResponseStatus._
import karate.io.netty.handler.codec.http._
import karate.io.netty.handler.stream.ChunkedStream
import karate.io.netty.util.CharsetUtil

class HttpPageHandler(enableKeepAlive: Boolean) extends SimpleChannelInboundHandler[FullHttpRequest] {

  override def channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    if (!req.decoderResult().isSuccess) {
      sendError(ctx, req, BAD_REQUEST)
    } else {
      val uri = req.uri()
      if (uri.startsWith("/api/")) {
        handleApiRequest(ctx, req)
      } else { // static pages
        if (!HttpMethod.GET.equals(req.method())) {
          sendError(ctx, req, METHOD_NOT_ALLOWED)
        } else {
          val sendIndexPage = () => {
            if (INDEX_FILE_URL == null) {
              sendError(ctx, req, NOT_FOUND)
            } else {
              sendFile(ctx, req, getSteamFromStaticResource(STATIC_INDEX_FILE_PATH), STATIC_INDEX_FILE_PATH)
            }
          }
          val path = sanitizeUri(req.uri())
          if (path == null || path.equals(STATIC_FOLDER_PATH)) {
            sendIndexPage()
          } else {
            val stream = getSteamFromStaticResource(path)
            if (stream != null) {
              sendFile(ctx, req, stream, path)
            } else {
              sendIndexPage()
            }
          }
        }
      }
    }
  }

  def handleApiRequest(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    try {
      req.method() match {
        case HttpMethod.GET => getHandler(ctx, req)
        case HttpMethod.POST => postHandler(ctx, req)
        case _ => sendError(ctx, req, HttpResponseStatus.NOT_FOUND)
      }
    } catch {
      case t: Throwable =>
        logger.error("handle api request error: ", t)
        sendApiResponse(ctx, req, ApiResError(t.getMessage))
    }
  }

  def getHandler(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    import CliSystem.ec
    val uri = new QueryStringDecoder(req.uri())
    val paths = uri.path().split("/")
    paths match { // ["", "api", ...]
      case Array(_, _, "devices") =>
        CliSystem.getDevices().map(data => sendApiResponse(ctx, req, data))
      case Array(_, _, "web", id) =>
        CliSystem.getTask(id).map(data => sendApiResponse(ctx, req, data))
      case _ => sendError(ctx, req, HttpResponseStatus.NOT_FOUND)
    }
  }

  def postHandler(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    val uri = new QueryStringDecoder(req.uri())
    val paths = uri.path().split("/")
    paths match { // ["", "api", ...]
      case Array(_, _, "run") =>
        val body = req.content().toString(StandardCharsets.UTF_8)
        if (body != null) {
          CliSystem.sendToPool(JsonUtils.parse(body, classOf[TaskInfo]))
          sendApiResponse(ctx, req, null)
        } else {
          sendError(ctx, req, HttpResponseStatus.BAD_REQUEST)
        }
      case _ => sendError(ctx, req, HttpResponseStatus.NOT_FOUND)
    }
  }

  def sanitizeUri(uri: String): String = {
    try {
      if (uri.equals("/")) {
        STATIC_FOLDER_PATH
      } else {
        var decodedUri = URLDecoder.decode(uri, CharsetUtil.UTF_8)
        if (decodedUri.isEmpty || decodedUri.charAt(0) != '/' || decodedUri.charAt(decodedUri.size - 1) == '/') {
          null
        } else {
          decodedUri = decodedUri.replace("/", File.separator)
          if (
            decodedUri.contains(s"${File.separator}.") ||
              decodedUri.contains(s".${File.separator}") ||
              decodedUri.charAt(0) == '.' || uri.charAt(uri.length - 1) == '.' ||
              INSECURE_URI.matcher(decodedUri).matches()
          ) {
            null
          } else {
            STATIC_FOLDER_NAME + decodedUri
          }
        }
      }
    } catch {
      case t: UnsupportedEncodingException =>
        logger.warn(LogUtils.stackTraceToString(t))
        null
    }
  }

  def sendFile(ctx: ChannelHandlerContext, req: FullHttpRequest, in: InputStream, path: String): Unit = {
    if (NettyUtils.ifFileModified(req, LAST_MODIFIED_TIME)) {
      sendNotModified(ctx, req)
    } else {
      val res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, OK)
      NettyUtils.setDateAndCacheHeaders(res, LAST_MODIFIED_TIME)
      res.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED)
      // write the initial line and header
      ctx.write(res)
      // write the content
      val sendFileFuture: ChannelFuture = ctx.writeAndFlush(
        new HttpChunkedInput(new ChunkedStream(in)),
        ctx.newProgressivePromise()
      )
      sendFileFuture.addListener(new ChannelProgressiveFutureListener {
        override def operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long): Unit = {
        }

        override def operationComplete(future: ChannelProgressiveFuture): Unit = {
          logger.debug(s"$path transfer complete")
        }
      })
      sendFileFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }

  def sendApiResponse(ctx: ChannelHandlerContext, req: FullHttpRequest, body: Object): Unit = {
    val response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
      Unpooled.copiedBuffer(JsonUtils.stringify(ApiRes(data = body)), CharsetUtil.UTF_8)
    )
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8")
    sendAndCleanupConnection(ctx, req, response)
  }

  def sendRedirect(ctx: ChannelHandlerContext, req: FullHttpRequest, newUrl: String): Unit = {
    val res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, FOUND, Unpooled.EMPTY_BUFFER)
    res.headers().set(HttpHeaderNames.LOCATION, newUrl)
    sendAndCleanupConnection(ctx, req, res)
  }

  def sendError(ctx: ChannelHandlerContext, req: FullHttpRequest, status: HttpResponseStatus): Unit = {
    val response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status,
      Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8)
    )
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
    sendAndCleanupConnection(ctx, req, response)
  }

  def sendNotModified(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {
    val res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER)
    NettyUtils.setDateHeader(res)
    sendAndCleanupConnection(ctx, req, res)
  }

  def sendAndCleanupConnection(ctx: ChannelHandlerContext, req: FullHttpRequest, res: FullHttpResponse): Unit = {
    if (enableKeepAlive) {
      val keepAlive = HttpUtil.isKeepAlive(req)
      HttpUtil.setContentLength(res, res.content().readableBytes())
      if (!keepAlive) {
        res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
      } else if (req.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
        res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      }
      val flushFuture = ctx.writeAndFlush(res)
      if (!keepAlive) {
        flushFuture.addListener(ChannelFutureListener.CLOSE)
      }
    } else {
      res.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
      ctx.writeAndFlush(res).addListener(ChannelFutureListener.CLOSE)
    }
  }

}

object HttpPageHandler {

  val logger = Logger("HttpPageHandler")
  val INSECURE_URI = Pattern.compile(".*[<>&\"].*")
  val STATIC_FOLDER_NAME = "static"
  val STATIC_FOLDER_PATH = "static/"
  val STATIC_INDEX_FILE_PATH = "static/index.html"
  val INDEX_FILE_URL: URL = getUrlFromStaticResource(STATIC_INDEX_FILE_PATH)
  val LAST_MODIFIED_TIME = System.currentTimeMillis()

  def getUrlFromStaticResource(path: String): URL = {
    getClass.getClassLoader.getResource(path)
  }

  def getSteamFromStaticResource(path: String): InputStream = {
    getClass.getClassLoader.getResourceAsStream(path)
  }

}
