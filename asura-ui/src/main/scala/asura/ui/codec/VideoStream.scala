package asura.ui.codec

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{BlockingQueue, LinkedBlockingDeque}

import asura.ui.codec.VideoStream.logger
import asura.ui.hub.Hubs.RenderingFrameHub
import asura.ui.hub.RawH264Packet
import com.typesafe.scalalogging.Logger
import org.bytedeco.ffmpeg.avcodec.{AVCodecContext, AVCodecParserContext, AVPacket}
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacpp.{BytePointer, IntPointer, Pointer}

case class VideoStream(device: String, decoder: Decoder) extends Runnable {

  private val sinks = RenderingFrameHub.getSinks(device)
  private val stopFlag = new AtomicBoolean(false)
  private val packets: BlockingQueue[RawH264Packet] = new LinkedBlockingDeque()
  private var codecCtx: AVCodecContext = null
  private var parser: AVCodecParserContext = null
  private var hasPending: Boolean = false
  private var pending: AVPacket = null
  private var thread: Thread = null

  def put(packet: RawH264Packet): Unit = {
    packets.put(packet)
  }

  def stop(): Unit = {
    stopFlag.set(true)
    if (this.thread != null) {
      this.thread.interrupt()
    }
  }

  def setThread(thread: Thread): Unit = {
    this.thread = thread
  }

  override def run(): Unit = {
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
    parser = avcodec.av_parser_init(avcodec.AV_CODEC_ID_H264)
    if (parser == null) {
      closeDecoder()
      freeCodecCtx()
      throw new RuntimeException("Could not initialize parser")
    }
    // We must only pass complete frames to av_parser_parse2()!
    // It's more complicated, but this allows to reduce the latency by 1 frame!
    parser.flags(parser.flags() | AVCodecParserContext.PARSER_FLAG_COMPLETE_FRAMES)
    try {
      while (!stopFlag.get()) {
        try {
          codecPacket(packets.take())
        } catch {
          case _: InterruptedException => logger.info(s"$device: Parser is stopped")
          case t: Throwable => logger.error(s"$device: {}", t)
        }
      }
    } catch {
      case t: Throwable => logger.error(s"$device: {}", t)
    }
    logger.info(s"$device: End of frames")
    if (hasPending) {
      avcodec.av_packet_unref(pending)
    }
    avcodec.av_parser_close(parser)
    clear()
  }

  def codecPacket(packet: RawH264Packet): Unit = {
    val avPacket = new AVPacket()
    avcodec.av_new_packet(avPacket, packet.size)
    avPacket.data().put(packet.buf: _*)
    // ScrcpyVideoHandler.NO_PTS
    avPacket.pts(if (packet.pts != -1) packet.pts else avutil.AV_NOPTS_VALUE)
    pushPacket(avPacket)
    avcodec.av_packet_unref(avPacket)
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
    logger.info(s"$device: Process config packet")
    // TODO: recorder , save sps,pps non-vcl
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
        //        if (listener != null) {
        //          listener.onDecoded(frame)
        //        }
        // TODO: ,swap renderingFrame, pub to hubs, renderingFrame
        RenderingFrameHub.write(sinks, frame)
      } else {
        throw new RuntimeException(s"Could not receive video frame: $ret")
      }
    } else {
      throw new RuntimeException("Parse2 error")
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
    closeDecoder()
    freeCodecCtx()
  }

}

object VideoStream {

  val logger = Logger(getClass)

  def init(device: String, renderExpiredFrames: Boolean = false): VideoStream = {
    val videoBuffer = VideoBuffer.init(renderExpiredFrames, FpsCounter.init())
    val decoder = Decoder.init(videoBuffer)
    VideoStream(device, decoder)
  }

  def startThread(device: String, renderExpiredFrames: Boolean = false): VideoStream = {
    val stream = init(device, renderExpiredFrames)
    val thread = new Thread(stream, s"video-decoder-$device")
    stream.setThread(thread)
    thread.start()
    stream
  }

}
