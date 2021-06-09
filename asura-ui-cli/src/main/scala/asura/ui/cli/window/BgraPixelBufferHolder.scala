package asura.ui.cli.window

import java.nio.ByteBuffer

import asura.ui.cli.codec.Size
import javafx.scene.image.{PixelBuffer, PixelFormat}
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.{avutil, swscale}
import org.bytedeco.ffmpeg.swscale.{SwsContext, SwsFilter}
import org.bytedeco.javacpp._

case class BgraPixelBufferHolder(buffer: PixelBuffer[ByteBuffer]) {

  val context: SwsContext = swscale.sws_getContext(
    buffer.getWidth, buffer.getHeight, avutil.AV_PIX_FMT_YUV420P,
    buffer.getWidth, buffer.getHeight, avutil.AV_PIX_FMT_BGRA,
    0, new SwsFilter(), new SwsFilter(), new DoublePointer()
  )
  val dst = new PointerPointer[Pointer](1).put(new BytePointer(buffer.getBuffer))
  val dstStride = new IntPointer(1L).put(buffer.getWidth * 4)

  def write(frame: AVFrame): Unit = {
    swscale.sws_scale(
      context, frame.data(), frame.linesize(),
      0, buffer.getHeight,
      dst, dstStride
    )
  }

  def free(): Unit = {
    swscale.sws_freeContext(context)
    dst.close()
    dstStride.close()
  }

}

object BgraPixelBufferHolder {

  def apply(size: Size): BgraPixelBufferHolder = apply(size.width, size.height)

  def apply(width: Int, height: Int): BgraPixelBufferHolder = {
    val buffer = ByteBuffer.allocateDirect(width * height * 4)
    val pixelBuffer = new PixelBuffer[ByteBuffer](width, height, buffer, PixelFormat.getByteBgraPreInstance())
    BgraPixelBufferHolder(pixelBuffer)
  }

}
