package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.model.CaseGenerator.CaseGeneratorListItem

case class CaseGenerator(
                          var script: String = StringUtils.EMPTY,
                          var list: Seq[CaseGeneratorListItem] = Nil,
                          var count: Int = 0 // case count will be generated: list's size plus one only if the script is valid
                        )


object CaseGenerator {

  case class CaseGeneratorListItem(map: Seq[KeyValueObject], assert: Map[String, Any])

}
