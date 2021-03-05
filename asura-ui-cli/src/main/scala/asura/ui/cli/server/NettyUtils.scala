package asura.ui.cli.server

import java.text.SimpleDateFormat
import java.util._

import asura.common.util.LogUtils
import com.typesafe.scalalogging.Logger
import io.netty.karate.buffer.Unpooled
import io.netty.karate.channel.{Channel, ChannelFutureListener}
import io.netty.karate.handler.codec.http.{FullHttpResponse, HttpHeaderNames, HttpRequest, HttpResponse}

object NettyUtils {

  val logger = Logger(getClass)
  val HTTP_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss zzz"
  val HTTP_DATE_GMT_TIMEZONE = "GMT+8"
  val HTTP_CACHE_SECONDS = 60

  def flushAndClose(ch: Channel): Unit = {
    if (ch.isActive) {
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }
  }

  def ifFileModified(req: HttpRequest, lastModified: Long): Boolean = {
    try {
      val ifModifiedSince = req.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE)
      if (ifModifiedSince != null && !ifModifiedSince.isEmpty) {
        val dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT)
        val date = dateFormatter.parse(ifModifiedSince)
        val ifModifiedSinceDateSeconds = date.getTime / 1000
        val fileLastModifiedSeconds = lastModified / 1000
        if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
          true
        } else {
          false
        }
      } else {
        false
      }
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        false
    }
  }

  def setDateHeader(res: FullHttpResponse): Unit = {
    val dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT)
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE))
    val calendar = new GregorianCalendar()
    res.headers().set(HttpHeaderNames.DATE, dateFormatter.format(calendar.getTime))
  }

  def setDateAndCacheHeaders(res: HttpResponse, lastModified: Long): Unit = {
    val dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT)
    dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE))
    val calendar = new GregorianCalendar()
    res.headers().set(HttpHeaderNames.DATE, dateFormatter.format(calendar.getTime))
    calendar.add(Calendar.SECOND, HTTP_CACHE_SECONDS)
    res.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(calendar.getTime))
    res.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS)
    res.headers().set(HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(lastModified)))
  }

}
