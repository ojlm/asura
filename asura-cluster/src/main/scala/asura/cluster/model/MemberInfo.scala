package asura.cluster.model

import akka.cluster.Member
import asura.common.util.StringUtils

case class MemberInfo(
                       roles: Seq[String],
                       address: String,
                       protocol: String,
                       port: Int,
                       status: String,
                     )

object MemberInfo {

  def fromMember(m: Member): MemberInfo = {
    MemberInfo(
      roles = m.roles.toSeq,
      protocol = m.address.protocol,
      address = m.address.host.getOrElse(StringUtils.EMPTY),
      port = m.address.port.getOrElse(0),
      status = m.status.toString,
    )
  }
}
