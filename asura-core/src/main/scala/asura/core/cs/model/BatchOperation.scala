package asura.core.cs.model

import asura.core.cs.model.BatchOperation.UpdateLabels
import asura.core.es.model.LabelRef

case class BatchOperation(
                           labels: Seq[UpdateLabels]
                         )

object BatchOperation {

  case class UpdateLabels(id: String, labels: Seq[LabelRef])

}
