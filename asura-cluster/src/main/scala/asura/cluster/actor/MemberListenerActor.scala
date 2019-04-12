package asura.cluster.actor

import akka.actor.Props
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member, MemberStatus}
import asura.cluster.actor.MemberListenerActor.GetAllMembers
import asura.cluster.model.MemberInfo
import asura.common.actor.BaseActor

class MemberListenerActor extends BaseActor {

  val cluster = Cluster(context.system)
  var nodes = Set.empty[Member]

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive: Receive = {
    case state: CurrentClusterState =>
      nodes = state.members.collect {
        case m if m.status == MemberStatus.Up => m
      }
    case MemberUp(member) =>
      log.info("Member({}) is Up: {}", member.roles.mkString(","), member.address)
      nodes += member
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
      nodes -= member
    case _: MemberEvent =>
    case GetAllMembers =>
      sender() ! nodes.map(MemberInfo.fromMember(_))
  }

}

object MemberListenerActor {
  def props() = Props(new MemberListenerActor())

  case class GetAllMembers()

}
