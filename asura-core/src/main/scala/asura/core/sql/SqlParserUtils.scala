package asura.core.sql

import com.alibaba.druid.sql.SQLUtils
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement

object SqlParserUtils {

  def isSelectStatement(sql: String): (Boolean, String) = {
    try {
      val statement = SQLUtils.parseSingleMysqlStatement(sql)
      statement match {
        case _: SQLSelectStatement => (true, null)
        case _ => (false, "Only allow select statement")
      }
    } catch {
      case t: Throwable => (false, t.getMessage)
    }
  }
}
