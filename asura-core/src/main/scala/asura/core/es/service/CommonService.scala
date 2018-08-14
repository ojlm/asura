package asura.core.es.service

import asura.core.es.model.FieldKeys

trait CommonService {

  val defaultIncludeFields = Seq(FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION)
}
