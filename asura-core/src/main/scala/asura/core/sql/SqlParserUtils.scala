package asura.core.sql

import asura.common.util.{LogUtils, StringUtils}
import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.statement.{SQLAlterTableStatement, SQLDropTableStatement, SQLExprTableSource, SQLSelectStatement}
import com.alibaba.druid.sql.dialect.mysql.ast.statement._
import com.typesafe.scalalogging.Logger

object SqlParserUtils {

  val logger = Logger("SqlParserUtils")

  def isSelectStatement(sql: String): (Boolean, String) = {
    try {
      val statement = SQLUtils.parseSingleMysqlStatement(sql)
      statement match {
        case _: SQLSelectStatement => (true, null)
        case _ => (false, null)
      }
    } catch {
      case t: Throwable => (false, t.getMessage)
    }
  }

  @throws[Throwable]
  def getStatementTable(sql: String): String = {
    try {
      val statement = SQLUtils.parseSingleMysqlStatement(sql)
      val tableName = statement match {
        case statement: MySqlCreateTableStatement =>
          val name = statement.getTableSource.getName
          if (null == name) StringUtils.EMPTY else name.getSimpleName
        case statement: SQLDropTableStatement =>
          val sb = new StringBuilder()
          statement.getTableSources.forEach(s => {
            val name = s.getName
            if (null != name) sb.append(name.getSimpleName).append(",")
          })
          if (sb.length > 0) sb.substring(0, sb.length - 1) else StringUtils.EMPTY
        case statement: SQLAlterTableStatement =>
          val table = statement.getTableName
          if (null != table) table else StringUtils.EMPTY
        case statement: MySqlInsertStatement =>
          val name = statement.getTableName
          if (null == name) StringUtils.EMPTY else name.getSimpleName
        case statement: MySqlDeleteStatement =>
          val table = statement.getTableName
          if (null != table) table.getSimpleName else StringUtils.EMPTY
        case statement: MySqlUpdateStatement =>
          val name = statement.getTableName
          if (null == name) StringUtils.EMPTY else name.getSimpleName
        case statement: SQLSelectStatement =>
          val name = statement.getSelect.getQuery.asInstanceOf[MySqlSelectQueryBlock]
            .getFrom.asInstanceOf[SQLExprTableSource]
            .getName
          if (null == name) StringUtils.EMPTY else name.getSimpleName
        case _ => StringUtils.EMPTY
      }
      tableName.toLowerCase
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        throw t
    }
  }
}
