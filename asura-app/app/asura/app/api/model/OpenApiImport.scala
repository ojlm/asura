package asura.app.api.model

import asura.core.api.openapi.ConvertOptions
import asura.core.es.model.HttpCaseRequest

case class OpenApiImport(
                          url: String,
                          content: String,
                          list: Seq[HttpCaseRequest],
                          options: ConvertOptions,
                        )
