package asura.app.api.model

import asura.core.es.model.DubboRequest
import asura.core.runtime.ContextOptions

case class TestDubbo(id: String, request: DubboRequest, options: ContextOptions)
