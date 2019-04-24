package asura.core.sql

import java.sql._
import java.util

import asura.common.util.LogUtils
import asura.core.es.model.SqlRequest
import com.typesafe.scalalogging.Logger

object MySqlConnector {

  val logger = Logger("MySqlConnectors")

  @throws[Throwable]
  def connect(sql: SqlRequest): Connection = {
    val url = s"jdbc:mysql://${sql.host}:${sql.port}/${sql.database}?useCursorFetch=true&useUnicode=true&characterEncoding=utf-8"
    try {
      DriverManager.getConnection(url, sql.username, sql.password)
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        throw t
    }
  }

  @throws[Throwable]
  def executeUpdate(conn: Connection, sql: String): Integer = {
    val statement = conn.createStatement()
    try {
      statement.executeUpdate(sql)
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        throw t
    } finally {
      statement.close()
    }
  }

  @throws[Throwable]
  def executeQuery(conn: Connection, sql: String): java.util.List[java.util.HashMap[String, Object]] = {
    var statement: Statement = null
    try {
      // https://stackoverflow.com/questions/26046234/is-there-a-mysql-jdbc-that-will-respect-fetchsize
      statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      statement.setFetchSize(SqlConfig.MAX_ROWS_SIZE)
      val rs = statement.executeQuery(sql)
      val list = new util.ArrayList[util.HashMap[String, Object]]()
      val metaData = rs.getMetaData
      val count = metaData.getColumnCount
      while (rs.next() && list.size() < SqlConfig.MAX_ROWS_SIZE) {
        val row = new util.HashMap[String, Object]()
        for (i <- 1 to count) {
          val value = preserve(metaData, rs, i)
          if (null != value) row.put(metaData.getColumnName(i), value)
        }
        list.add(row)
      }
      list
    } finally {
      if (null != statement) statement.close()
    }
  }

  private def preserve(meta: ResultSetMetaData, rs: ResultSet, col: Int): Object = {
    val className = meta.getColumnClassName(col)
    className match {
      case "java.lang.Long" | "java.lang.Integer" | "java.lang.Short" | "java.lang.Byte"
           | "java.lang.Boolean"
      =>
        rs.getObject(col)
      case _ =>
        rs.getObject(col).toString
    }
  }
}
