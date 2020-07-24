package asura.core.api.openapi

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.core.es.model.HttpCaseRequest

case class ConvertResults(
                           error: ErrorMessage = null,
                           list: Seq[HttpCaseRequest] = Nil
                         )
