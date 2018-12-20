package asura.app.api.model

import asura.core.es.model.DomainOnlineConfig

case class PreviewOnlineApi(
                             config: DomainOnlineConfig,
                             domainTotal: Long,
                           )
