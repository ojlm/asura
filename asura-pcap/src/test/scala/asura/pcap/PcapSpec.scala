package asura.pcap

import java.net.InetAddress
import java.util.concurrent.Executors

import org.pcap4j.core.BpfProgram.BpfCompileMode
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode
import org.pcap4j.core.{PacketListener, PcapNetworkInterface, Pcaps}
import org.pcap4j.packet.{IpV4Packet, Packet, TcpPacket}
import org.scalatest._

class PcapSpec extends FunSuite {

  test("find-devices") {
    val interfaces = Pcaps.findAllDevs()
    println(interfaces)
  }

  test("capture packet") {
    // find network interface
    val address = InetAddress.getByName("192.168.3.5")
    val nif: PcapNetworkInterface = Pcaps.getDevByAddress(address)
    println(nif)

    // open packet capture handle
    val snapLen = 65536
    val mode = PromiscuousMode.NONPROMISCUOUS
    val timeout = 0
    val handle = nif.openLive(snapLen, mode, timeout)

    // capture packet
    val packet: Packet = handle.getNextPacketEx
    val ipV4packet = packet.get(classOf[IpV4Packet])
    val srcAddr = ipV4packet.getHeader.getSrcAddr
    println(srcAddr)
  }

  test("loop") {
    val nif = Pcaps.getDevByName("en0")
    println(s"${nif.getName}(${nif.getDescription}): ${nif.getAddresses}, ${nif.getLinkLayerAddresses}")

    val handle = nif.openLive(65536, PromiscuousMode.PROMISCUOUS, 10)
    handle.setFilter("tcp", BpfCompileMode.OPTIMIZE)
    val listener = new PacketListener {
      override def gotPacket(packet: Packet): Unit = {
        val ipPacket = packet.get(classOf[IpV4Packet])
        if (null != ipPacket) {
          val ipHeader = ipPacket.getHeader
          val srcAddr = ipHeader.getSrcAddr.getHostAddress
          val dstAddr = ipHeader.getDstAddr.getHostAddress
          val tcpPacket = ipPacket.get(classOf[TcpPacket])
          var srcPort = 0
          var dstPort = 0
          if (null != tcpPacket) {
            val tcpHeader = tcpPacket.getHeader
            srcPort = tcpHeader.getSrcPort.valueAsInt()
            dstPort = tcpHeader.getDstPort.valueAsInt()
          }
          println(s"${handle.getTimestamp}  ${srcAddr}:${srcPort} => ${dstAddr}:${dstPort}  ${tcpPacket.getClass.getName}")
        }
      }
    }

    try {
      val pool = Executors.newCachedThreadPool()
      handle.loop(100, listener, pool)
      val stats = handle.getStats
      println(s"recv: ${stats.getNumPacketsReceived}")
      println(s"drop: ${stats.getNumPacketsDropped}")
      println(s"ifdrop: ${stats.getNumPacketsDroppedByIf}")
    } catch {
      case t: Throwable =>
        throw t
    } finally {
      handle.close()
    }
  }
}
