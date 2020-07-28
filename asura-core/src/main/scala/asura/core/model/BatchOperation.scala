package asura.core.model

import asura.core.es.model.Label.LabelRef

object BatchOperation {

  case class BatchDelete(ids: Seq[String])

  case class BatchTransfer(group: String, project: String, ids: Seq[String])

  case class BatchOperationLabels(
                                   labels: Seq[UpdateLabels]
                                 )

  case class UpdateLabels(id: String, labels: Seq[LabelRef])

}
