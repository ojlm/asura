package asura.core.notify

import asura.common.exceptions.ErrorMessages.ErrorMessage

case class NotifyResponse(isOk: Boolean, subscriber: String, error: ErrorMessage = null)
