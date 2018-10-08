package asura.core.notify

import asura.core.ErrorMessages

case class NotifyResponse(isOk: Boolean, subscriber: String, error: ErrorMessages.ErrorMessage = null)
