package asura.core

import akka.actor.{ActorRef, ActorSystem}
import asura.core.sql.actor.SqlRequestInvokerActor
import asura.dubbo.actor.GenericServiceInvokerActor

object RunnerActors {

  var dubboInvoker: ActorRef = null
  var sqlInvoker: ActorRef = null

  def init(system: ActorSystem): Unit = {
    dubboInvoker = system.actorOf(GenericServiceInvokerActor.props(), "dubbo-invoker")
    sqlInvoker = system.actorOf(SqlRequestInvokerActor.props(), "sql-invoker")
  }
}
