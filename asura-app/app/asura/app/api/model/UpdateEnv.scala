package asura.app.api.model

import asura.core.es.model.Environment

case class UpdateEnv(
                      id: String,
                      env: Environment
                    )
