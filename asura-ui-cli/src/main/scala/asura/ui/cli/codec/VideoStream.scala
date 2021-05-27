package asura.ui.cli.codec

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{BlockingQueue, LinkedBlockingDeque}

import asura.ui.cli.codec.VideoStream.logger
import asura.ui.cli.hub.StreamFrame
import asura.ui.cli.server.ScrcpyStreamHandler
import com.typesafe.scalalogging.Logger
import org.bytedeco.ffmpeg.avcodec.{AVCodecContext, AVCodecParserContext, AVPacket}
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacpp.{BytePointer, IntPointer, Pointer}

case class VideoStream(
                        decoder: Decoder,
                        recorder: Recorder,
                        listener: StreamListener,
                      ) {

  val stop = new AtomicBoolean(false)
  val frames: BlockingQueue[StreamFrame] = new LinkedBlockingDeque()
  var codecCtx: AVCodecContext = null
  var parser: AVCodecParserContext = null
  var hasPending: Boolean = false
  var pending: AVPacket = null

  def put(frame: StreamFrame): Unit = {
    frames.put(frame)
  }

  def run(): Unit = {
    val codec = avcodec.avcodec_find_decoder(avcodec.AV_CODEC_ID_H264)
    if (codec == null) {
      throw new RuntimeException("H.264 decoder not found")
    }
    codecCtx = avcodec.avcodec_alloc_context3(codec)
    if (codecCtx == null) {
      throw new RuntimeException("Could not allocate codec context")
    }
    if (decoder == null || !decoder.open(codec)) {
      freeCodecCtx()
      throw new RuntimeException("Could not open decoder")
    }
    if (recorder != null) {
      if (!recorder.open(codec)) {
        closeDecoder()
        freeCodecCtx()
        throw new RuntimeException("Could not open recorder")
      }
      if (!recorder.start()) {
        closeRecorder()
        closeDecoder()
        freeCodecCtx()
        throw new RuntimeException("Could not start recorder")
      }
    }
    parser = avcodec.av_parser_init(avcodec.AV_CODEC_ID_H264)
    if (parser == null) {
      stopAndJoinRecorder()
      closeRecorder()
      closeDecoder()
      freeCodecCtx()
      throw new RuntimeException("Could not initialize parser")
    }
    // We must only pass complete frames to av_parser_parse2()!
    // It's more complicated, but this allows to reduce the latency by 1 frame!
    parser.flags(parser.flags() | AVCodecParserContext.PARSER_FLAG_COMPLETE_FRAMES)
    try {
      while (!stop.get()) {
        try {
          codecFrame(frames.take())
        } catch {
          case t: Throwable => logger.error("", t)
        }
      }
    } catch {
      case t: InterruptedException =>
        logger.error("Interrupted: {}", t)
      case t: Throwable =>
        logger.error("{}", t)
    }
    logger.info("End of frames")
    if (hasPending) {
      avcodec.av_packet_unref(pending)
    }
    avcodec.av_parser_close(parser)
    clear()
  }

  def codecFrame(frame: StreamFrame): Unit = {
    val packet = new AVPacket()
    avcodec.av_new_packet(packet, frame.size)
    packet.data().put(frame.buf: _*)
    packet.pts(if (frame.pts != ScrcpyStreamHandler.NO_PTS) frame.pts else avutil.AV_NOPTS_VALUE)
    pushPacket(packet)
    avcodec.av_packet_unref(packet)
  }

  def pushPacket(packet: AVPacket): Unit = {
    var dealPacket = packet
    val isConfig = dealPacket.pts() == avutil.AV_NOPTS_VALUE
    // A config packet must not be decoded immediately (it contains no
    // frame); instead, it must be concatenated with the future data packet.
    if (hasPending || isConfig) {
      var offset = 0
      if (hasPending) {
        offset = pending.size()
        if (avcodec.av_grow_packet(pending, dealPacket.size()) != 0) {
          throw new RuntimeException("Could not grow packet")
        }
      } else {
        pending = new AVPacket()
        avcodec.av_new_packet(pending, dealPacket.size())
        hasPending = true
      }
      Pointer.memcpy(pending.data().position(offset), dealPacket.data(), dealPacket.size())
      if (!isConfig) {
        // prepare the concat packet to send to the decoder
        pending.pts(dealPacket.pts())
        pending.dts(dealPacket.dts())
        pending.flags(dealPacket.flags())
        dealPacket = pending
      }
    }
    if (isConfig) {
      // config packet
      processConfigPacket(dealPacket)
    } else {
      // data packet
      processDataPacket(dealPacket)
      if (hasPending) {
        // the pending packet must be discarded (consumed or error)
        hasPending = false
        avcodec.av_packet_unref(pending)
      }
    }
  }

  def processConfigPacket(packet: AVPacket): Unit = {
    logger.info("process config packet")
    // TODO: recorder
  }

  def processDataPacket(packet: AVPacket): Unit = {
    val inData = packet.data()
    val inLen = packet.size()
    val outData = new BytePointer() // NULL
    val outLen = new IntPointer(1L)
    val ret = avcodec.av_parser_parse2(parser, codecCtx,
      outData, outLen, inData, inLen,
      avutil.AV_NOPTS_VALUE, avutil.AV_NOPTS_VALUE, -1
    )
    if (ret == inLen && inLen == outLen.get()) {
      if (parser.key_frame() == 1) {
        packet.flags(packet.flags() | avcodec.AV_PKT_FLAG_KEY)
      }
      var ret = avcodec.avcodec_send_packet(decoder.codecCtx, packet)
      if (ret < 0) {
        throw new RuntimeException(s"Could not send video packet: $ret")
      }
      ret = avcodec.avcodec_receive_frame(decoder.codecCtx, decoder.videoBuffer.decodingFrame)
      if (ret == 0) {
        // a frame was received
        val frame = decoder.videoBuffer.decodingFrame
        if (listener != null) {
          listener.onDecoded(frame)
        }
      } else {
        throw new RuntimeException(s"Could not receive video frame: $ret")
      }
    } else {
      throw new RuntimeException("Parse2 error")
    }
  }

  def stopAndJoinRecorder(): Unit = {
    if (recorder != null) {
      recorder.stop()
      logger.info("Finishing recording...")
      recorder.join()
    }
  }

  def closeRecorder(): Unit = {
    if (recorder != null) {
      recorder.close()
    }
  }

  def closeDecoder(): Unit = {
    if (decoder != null) {
      decoder.close()
    }
  }

  def freeCodecCtx(): Unit = {
    if (codecCtx != null) {
      avcodec.avcodec_free_context(codecCtx)
    }
  }

  def clear(): Unit = {
    stopAndJoinRecorder()
    closeRecorder()
    closeDecoder()
    freeCodecCtx()
  }

}

object VideoStream {

  val logger = Logger(getClass)

  def init(recorder: Recorder, listener: StreamListener): VideoStream = {
    val videoBuffer = VideoBuffer.init(false, FpsCounter.init())
    val decoder = Decoder.init(videoBuffer)
    VideoStream(decoder, recorder, listener)
  }

}
