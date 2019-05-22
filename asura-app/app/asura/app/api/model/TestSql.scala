package asura.app.api.model

import asura.core.es.model.SqlRequest
import asura.core.runtime.ContextOptions

case class TestSql(id: String, request: SqlRequest, options: ContextOptions)
