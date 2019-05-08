package asura.core.job

/**
  * @param result :
  *               [[asura.core.http.HttpResult]],
  *               [[asura.core.sql.SqlResult]],
  *               [[asura.core.dubbo.DubboResult]]
  */
case class JobReportItemResultEvent(index: Int, status: String, errMsg: String = null, result: Any = null)
