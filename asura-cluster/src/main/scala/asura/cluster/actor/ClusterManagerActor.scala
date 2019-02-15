package asura.cluster.actor

import akka.actor.Props
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import asura.cluster.actor.ClusterManagerActor.GetAllMembers
import asura.cluster.model.MemberInfo
import asura.common.actor.BaseActor

class ClusterManagerActor extends BaseActor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember]
    )
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("Member({}) is Up: {}", member.roles.mkString(","), member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent =>
    case GetAllMembers =>
      sender() ! cluster.state.members.map(MemberInfo.fromMember(_))
  }

}

object ClusterManagerActor {
  def props() = Props(new ClusterManagerActor())

  case class GetAllMembers()

}
