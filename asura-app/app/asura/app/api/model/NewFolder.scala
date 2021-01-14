package asura.app.api.model

import asura.core.es.model.DocRef

case class NewFolder(
                      name: String,
                      description: String,
                      parent: String,
                      path: Seq[DocRef],
                    )
