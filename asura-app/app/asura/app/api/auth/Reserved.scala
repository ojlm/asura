package asura.app.api.auth

import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object Reserved {

  var groups: Set[String] = Set.empty

  def initReservedData(): Unit = {
    val config = ConfigFactory.load("reserved.conf")
    val groups = config.getStringList("asura.reserved.groups")
    val groupsSet = mutable.Set[String]()
    groups.forEach(g => groupsSet += g)
    Reserved.groups = groupsSet.toSet
  }

  def isReservedGroup(group: String): Boolean = {
    groups.contains(group)
  }
}
