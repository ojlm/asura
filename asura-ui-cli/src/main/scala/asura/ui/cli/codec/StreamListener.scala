package asura.ui.cli.codec

import org.bytedeco.ffmpeg.avutil.AVFrame

trait StreamListener extends AutoCloseable {

  def onDecoded(frame: AVFrame): Unit

  override def close(): Unit = {}

}
