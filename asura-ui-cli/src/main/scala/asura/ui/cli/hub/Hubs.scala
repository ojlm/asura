package asura.ui.cli.hub

object Hubs {

  // receive video stream
  val StreamHub = new Hub[StreamFrame]()
  // send control messages to device
  val ControllerHub = new Hub[ControlMessage]()
  // receive device messages
  val ReceiverHub = new Hub[DeviceMessage]()

}
