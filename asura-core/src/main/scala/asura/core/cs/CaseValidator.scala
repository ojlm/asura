package asura.core.cs

import asura.common.model.BoolErrorRes
import asura.common.util.StringUtils
import asura.core.es.model.Case

object CaseValidator {

  def check(c: Case, ns: String): BoolErrorRes = {
    if (!StringUtils.isEmpty(ns)) {
      (false, "empty namespace")
    } else {
      val (isOk, errMsg) = check(c)
      if (!isOk) {
        (false, errMsg)
      } else {
        (true, null)
      }
    }
  }

  def check(c: Case): BoolErrorRes = {
    if (null == c) {
      (false, "empty case")
    } else if (StringUtils.isEmpty(c.summary)) {
      (false, "empty summary")
    } else if (null == c.request) {
      (false, "empty request")
    } else if (StringUtils.isEmpty(c.api)) {
      (false, "empty api")
    } else if (StringUtils.isEmpty(c.project)) {
      (false, "empty project")
    } else if (StringUtils.isEmpty(c.group)) {
      (false, "empty group")
    } else {
      val req = c.request
      if (StringUtils.isEmpty(req.contentType)) {
        req.contentType = StringUtils.EMPTY
      }
      if (null == req.query) {
        req.query = Nil
      }
      if (null == req.cookie) {
        req.cookie = Nil
      }
      if (null == req.header) {
        req.header = Nil
      }
      (true, null)
    }
  }
}
