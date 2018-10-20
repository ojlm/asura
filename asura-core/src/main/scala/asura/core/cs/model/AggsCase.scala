package asura.core.cs.model

import asura.common.util.StringUtils
import asura.core.es.model.FieldKeys

case class AggsCase(
                     group: String,
                     project: String,
                     creator: String,
                     size: Int = 10
                   ) {

  def pageSize(): Int = if (Option(size).isDefined && size > 0) size else 10

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
