package asura.app.api.model

import asura.ui.model.ServoAddress

case class RunTaskInBlob(
                          key: String,
                          servos: Seq[ServoAddress]
                        )
