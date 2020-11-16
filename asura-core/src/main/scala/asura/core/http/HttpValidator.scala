package asura.core.http

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.es.model.HttpStepRequest

object HttpValidator {

  def check(c: HttpStepRequest, ns: String): ErrorMessage = {
    if (!StringUtils.isEmpty(ns)) {
      ErrorMessages.error_EmptyNamespace
    } else {
      check(c)
    }
  }

  def check(c: HttpStepRequest): ErrorMessage = {
    if (null == c) {
      ErrorMessages.error_EmptyCase
    } else if (StringUtils.isEmpty(c.summary)) {
      ErrorMessages.error_EmptySummary
    } else if (null == c.request) {
      ErrorMessages.error_EmptyRequest
    } else if (StringUtils.isEmpty(c.project)) {
      ErrorMessages.error_EmptyProject
    } else if (StringUtils.isEmpty(c.group)) {
      ErrorMessages.error_EmptyGroup
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
      null
    }
  }

  def check(cs: Seq[HttpStepRequest]): ErrorMessage = {
    var error: ErrorMessage = null
    var hasError = false
    for (c <- cs if !hasError) {
      error = check(c)
      if (null != error) hasError = true
    }
    error
  }
}
