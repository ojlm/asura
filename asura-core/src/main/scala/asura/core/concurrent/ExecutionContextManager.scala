package asura.core.concurrent

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

/**
  * Execution context used in this project
  *
  * `Global` [[ExecutionContextManager.sysGlobal]]:
  * 1. gRpc server,  `asura.rpc.grpc.impl.ServerManager`
  * 2. Future handler of gRpc server implements in `asura.rpc.grpc.impl`
  * 3. Job result deal in `asura.scheduler.AbstractJob`
  * 4. Slick future onComplete handler e.g. `asura.app.jobs.DumpDbJob`
  * 5. elastic4s default executor in `es` package
  *
  * Actor System dispatcher `asura.GlobalImplicits.dispatcher`:
  * 1. HttpEngine see `asura.http.HttpEngine`, actor system
  *
  * CachedExecutor for short asynchronous tasks
  * 1. sso check `asura.actor.playground.dandelion.DandelionTask`
  * 2. add job
  * 3. redis error handler
  **/
object ExecutionContextManager {

  implicit val sysGlobal = ExecutionContext.global
  implicit val cachedExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
}
