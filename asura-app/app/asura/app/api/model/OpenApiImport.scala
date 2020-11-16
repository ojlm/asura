package asura.app.api.model

import asura.core.api.openapi.ConvertOptions
import asura.core.es.model.HttpStepRequest

case class OpenApiImport(
                          url: String,
                          content: String,
                          list: Seq[HttpStepRequest],
                          options: ConvertOptions,
                        )
