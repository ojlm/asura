package asura.core.job

import asura.core.runtime.AbstractResult

/**
  * @param result :
  *               [[asura.core.http.HttpResult]],
  *               [[asura.core.sql.SqlResult]],
  *               [[asura.core.dubbo.DubboResult]]
  */
case class JobReportItemResultEvent(index: Int, status: String, errMsg: String = null, result: AbstractResult = null)
