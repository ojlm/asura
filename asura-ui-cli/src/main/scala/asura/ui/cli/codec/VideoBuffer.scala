package asura.ui.cli.codec

import org.bytedeco.ffmpeg.avutil.AVFrame
import org.bytedeco.ffmpeg.global.avutil

case class VideoBuffer(
                        decodingFrame: AVFrame,
                        renderingFrame: AVFrame,
                        renderExpiredFrames: Boolean,
                        interrupted: Boolean,
                        renderingFrameConsumed: Boolean,
                        fpsCounter: FpsCounter,
                      ) {

  def free(): Unit = {
    avutil.av_frame_free(decodingFrame)
    avutil.av_frame_free(renderingFrame)
  }

}

object VideoBuffer {

  def init(renderExpiredFrames: Boolean, fpsCounter: FpsCounter): VideoBuffer = {
    val decodingFrame = avutil.av_frame_alloc()
    if (decodingFrame != null) {
      val renderingFrame = avutil.av_frame_alloc()
      if (renderExpiredFrames != null) {
        VideoBuffer(decodingFrame, renderingFrame, renderExpiredFrames, false, true, fpsCounter)
      } else {
        avutil.av_frame_free(decodingFrame)
        null
      }
    } else {
      null
    }
  }

}
