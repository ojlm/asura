package asura.app.api.model

import asura.core.es.model.DocRef

case class NewFile(
                    name: String,
                    description: String,
                    parent: String,
                    path: Seq[DocRef],
                    app: String,
                    data: Map[String, Any] = null,
                  )
