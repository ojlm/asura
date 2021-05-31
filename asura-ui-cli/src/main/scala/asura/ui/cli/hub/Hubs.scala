package asura.ui.cli.hub

import org.bytedeco.ffmpeg.avutil.AVFrame

object Hubs {

  // receive video stream
  val RawH264StreamHub = new Hub[RawH264Packet]()
  // send control messages to device
  val ControllerHub = new Hub[ControlMessage]()
  // receive device messages
  val ReceiverHub = new Hub[DeviceMessage]()
  // rendering frame, send to web,windows,recorder...
  val RenderingFrameHub = new Hub[AVFrame]

}
