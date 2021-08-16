package asura.ui.codec

import asura.ui.hub.Sink
import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.{avutil, swscale}
import org.bytedeco.ffmpeg.swscale.{SwsContext, SwsFilter}
import org.bytedeco.javacpp.{DoublePointer, IntPointer, Pointer, PointerPointer}
import org.bytedeco.opencv.opencv_core.Mat
import org.opencv.core.CvType

abstract class ToMatStreamListener(width: Int, height: Int) extends Sink[AVFrame] {

  val context: SwsContext = swscale.sws_getContext(
    width, height, avutil.AV_PIX_FMT_YUV420P,
    width, height, avutil.AV_PIX_FMT_BGR24,
    0, new SwsFilter(), new SwsFilter(), new DoublePointer()
  )
  val buffer = new Mat(height, width, CvType.CV_8UC3)
  val dst = new PointerPointer[Pointer](1L).put(buffer.data())
  val dstStride = new IntPointer(1L).put(buffer.step1().toInt)

  override def write(frame: AVFrame): Boolean = {
    swscale.sws_scale(
      context, frame.data(), frame.linesize(),
      0, height,
      dst, dstStride
    )
    onUpdate(buffer)
  }

  override def close(): Unit = {
    swscale.sws_freeContext(context)
    buffer.close()
    dst.close()
    dstStride.close()
  }

  def onUpdate(frame: Mat): Boolean

}
