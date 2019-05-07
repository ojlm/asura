package asura.app.api.model

import asura.core.runtime.ContextOptions
import asura.core.es.model.HttpCaseRequest

case class TestCase(id: String, cs: HttpCaseRequest, options: ContextOptions)
