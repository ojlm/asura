package asura.ui.cli.hub

import asura.ui.cli.codec.VideoStream
import org.bytedeco.ffmpeg.avutil.AVFrame

case class RenderingFrameMessage(
                                  stream: VideoStream,
                                  frame: AVFrame,
                                )
