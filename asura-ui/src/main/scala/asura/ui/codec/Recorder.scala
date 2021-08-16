package asura.ui.codec

import asura.ui.codec.Recorder.RecordFormat.RecordFormat
import asura.ui.codec.Recorder.{Queue, RecordPacket}
import org.bytedeco.ffmpeg.avcodec.{AVCodec, AVPacket}
import org.bytedeco.ffmpeg.avformat.AVFormatContext

case class Recorder(
                     filename: String,
                     format: RecordFormat,
                     frameSize: Size,
                     var ctx: AVFormatContext = null,
                     var headerWritten: Boolean = false,
                     var stopped: Boolean = false, // set on recorder_stop() by the stream reader
                     var failed: Boolean = false, // set on packet write failure
                     queue: Queue[RecordPacket] = Queue(null, null),
                     // we can write a packet only once we received the next one so that we can
                     // set its duration (next_pts - current_pts)
                     // "previous" is only accessed from the recorder thread, so it does not
                     // need to be protected by the mutex
                     var previous: RecordPacket = null,
                   ) {

  def open(codec: AVCodec): Boolean = {
    // TODO
    false
  }

  def start(): Boolean = {
    // TODO
    false
  }

  def stop(): Unit = {

  }

  def join(): Unit = {

  }

  def close(): Boolean = {
    // TODO
    false
  }

}

object Recorder {

  object RecordFormat extends Enumeration {
    type RecordFormat = Value
    val AUTO, MP4, MKV = Value
  }

  case class RecordPacket(
                           packet: AVPacket,
                           next: RecordPacket,
                         )

  case class Queue[T](fist: T, last: T) {
    def isEmpty = fist != null
  }

  def init(filename: String, format: RecordFormat, frameSize: Size): Recorder = {
    Recorder(filename, format, frameSize)
  }

}
