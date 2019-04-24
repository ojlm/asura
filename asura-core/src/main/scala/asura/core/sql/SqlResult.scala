package asura.core.sql

import asura.core.cs.assertion.engine.Statistic

case class SqlResult(
                      val context: Object,
                      var statis: Statistic = Statistic(),
                      var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP,
                    ) {

}
