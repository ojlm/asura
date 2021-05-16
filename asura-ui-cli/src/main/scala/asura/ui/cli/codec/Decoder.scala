package asura.ui.cli.codec

import asura.ui.cli.codec.Decoder.logger
import com.typesafe.scalalogging.Logger
import org.bytedeco.ffmpeg.avcodec.{AVCodec, AVCodecContext}
import org.bytedeco.ffmpeg.avutil.AVDictionary
import org.bytedeco.ffmpeg.global.avcodec

case class Decoder(
                    videoBuffer: VideoBuffer,
                    var codecCtx: AVCodecContext,
                  ) {

  def open(codec: AVCodec): Boolean = {
    codecCtx = avcodec.avcodec_alloc_context3(codec)
    if (codecCtx == null) {
      logger.error("Could not allocate decoder context")
      false
    } else {
      if (avcodec.avcodec_open2(codecCtx, codec, new AVDictionary()) < 0) {
        logger.error("Could not open codec")
        avcodec.avcodec_free_context(codecCtx)
        false
      } else {
        true
      }
    }
  }

  def close(): Unit = {
    if (codecCtx != null) {
      avcodec.avcodec_close(codecCtx)
      avcodec.avcodec_free_context(codecCtx)
    }
  }

}

object Decoder {

  val logger = Logger(getClass)

  def init(videoBuffer: VideoBuffer): Decoder = {
    Decoder(videoBuffer, null)
  }

}
