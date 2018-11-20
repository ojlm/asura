package asura.core.cs.model

import asura.common.util.StringUtils
import asura.core.es.model.FieldKeys

case class AggsCase(
                     group: String,
                     project: String,
                     creator: String,
                     interval: String = null,
                     termsField: String = null,
                     dateRange: String = null,
                     size: Int = 10,
                   ) {

  def pageSize(): Int = if (Option(size).isDefined && size > 0) size else 10

  def aggInterval(): String = {
    if (StringUtils.isNotEmpty(interval)) interval else "1M"
  }

  def aggTermsField(): String = {
    if (StringUtils.isNotEmpty(termsField)) termsField else FieldKeys.FIELD_GROUP
  }

  def aggField(): String = {
    if (StringUtils.isNotEmpty(project)) {
      FieldKeys.FIELD_CREATOR
    } else if (StringUtils.isNotEmpty(group)) {
      FieldKeys.FIELD_PROJECT
    } else {
      FieldKeys.FIELD_GROUP
    }
  }
}
