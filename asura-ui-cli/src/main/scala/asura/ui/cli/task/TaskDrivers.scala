package asura.ui.cli.task

import scala.collection.mutable

case class TaskDrivers(
                        drivers: mutable.Set[TaskDriver] = mutable.Set[TaskDriver]()
                      )
