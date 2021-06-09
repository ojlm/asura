package asura.ui.cli.window

import java.nio.ByteBuffer

import asura.ui.cli.hub.Sink
import javafx.scene.image.PixelBuffer
import org.bytedeco.ffmpeg.avutil.AVFrame

abstract class BgraPixelBufferStreamListener(buffer: PixelBuffer[ByteBuffer]) extends Sink[AVFrame] {

  val holder = BgraPixelBufferHolder(buffer)

  override def write(frame: AVFrame): Boolean = {
    holder.write(frame)
    onUpdate(holder.buffer)
  }

  override def close(): Unit = {
    holder.free()
  }

  def onUpdate(frame: PixelBuffer[ByteBuffer]): Boolean

}
