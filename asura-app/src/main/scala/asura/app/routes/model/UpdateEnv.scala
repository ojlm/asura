package asura.routes.model

import asura.core.es.model.Environment

case class UpdateEnv(
                      id: String,
                      env: Environment
                    )
