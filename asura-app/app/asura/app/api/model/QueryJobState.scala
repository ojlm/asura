package asura.app.api.model

import asura.app.api.model.QueryJobState.QueryItem


case class QueryJobState(
                          items: Seq[QueryItem]
                        )

object QueryJobState {

  case class QueryItem(
                        group: String,
                        project: String,
                        jobId: String,
                      )

}
