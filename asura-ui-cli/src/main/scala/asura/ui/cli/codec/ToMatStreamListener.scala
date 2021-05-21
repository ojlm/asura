package asura.ui.cli.codec

import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.{avutil, swscale}
import org.bytedeco.ffmpeg.swscale.{SwsContext, SwsFilter}
import org.bytedeco.javacpp.{DoublePointer, IntPointer, Pointer, PointerPointer}
import org.bytedeco.opencv.opencv_core.Mat
import org.opencv.core.CvType

abstract class ToMatStreamListener(width: Int, height: Int) extends StreamListener {

  val context: SwsContext = swscale.sws_getContext(
    width, height, avutil.AV_PIX_FMT_YUV420P,
    width, height, avutil.AV_PIX_FMT_BGR24,
    0, new SwsFilter(), new SwsFilter(), new DoublePointer()
  )

  override def onDecoded(frame: AVFrame): Unit = {
    val image = new Mat(height, width, CvType.CV_8UC3)
    val dst = new PointerPointer[Pointer](1L)
    dst.put(image.data())
    val step = image.step1().toInt
    val dstStride = new IntPointer(1L)
    dstStride.put(step)
    swscale.sws_scale(
      context, frame.data(), frame.linesize(),
      0, height,
      dst, dstStride
    )
    onOpenCvFrame(image)
  }


  override def close(): Unit = {
    swscale.sws_freeContext(context)
  }

  def onOpenCvFrame(frame: Mat): Unit

}
