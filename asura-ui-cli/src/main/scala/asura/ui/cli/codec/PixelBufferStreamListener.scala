package asura.ui.cli.codec

import java.nio.ByteBuffer

import javafx.scene.image.PixelBuffer
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.{avutil, swscale}
import org.bytedeco.ffmpeg.swscale.{SwsContext, SwsFilter}
import org.bytedeco.javacpp._

abstract class PixelBufferStreamListener(buffer: PixelBuffer[ByteBuffer]) extends StreamListener {

  val width = buffer.getWidth
  val height = buffer.getHeight
  val context: SwsContext = swscale.sws_getContext(
    width, height, avutil.AV_PIX_FMT_YUV420P,
    width, height, avutil.AV_PIX_FMT_BGRA,
    0, new SwsFilter(), new SwsFilter(), new DoublePointer()
  )
  val dst = new PointerPointer[Pointer](1).put(new BytePointer(buffer.getBuffer))
  val dstStride = new IntPointer(1L).put(width * 4)

  override def onDecoded(frame: AVFrame): Unit = {
    swscale.sws_scale(
      context, frame.data(), frame.linesize(),
      0, height,
      dst, dstStride
    )
    onUpdate(buffer)
  }

  override def close(): Unit = {
    swscale.sws_freeContext(context)
    dst.close()
    dstStride.close()
  }

  def onUpdate(frame: PixelBuffer[ByteBuffer]): Unit

}
