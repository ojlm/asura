package asura.core.sql

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object SqlConfig {

  val MAX_ROWS_SIZE = 100
  val DEFAULT_MYSQL_CONNECTOR_CACHE_SIZE = 10

  val SQL_EC = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
}
