package asura.app.api.model

import asura.ui.model.ServerAddress

case class RunTaskInBlob(
                          key: String,
                          servers: Seq[ServerAddress]
                        )
