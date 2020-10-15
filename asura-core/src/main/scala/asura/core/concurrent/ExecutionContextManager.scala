package asura.core.concurrent

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object ExecutionContextManager {

  implicit val sysGlobal = ExecutionContext.global
  implicit val cachedExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
}
