package asura.ui.hub

import asura.ui.codec.VideoStream
import org.bytedeco.ffmpeg.avutil.AVFrame

case class RenderingFrameMessage(
                                  stream: VideoStream,
                                  frame: AVFrame,
                                )
