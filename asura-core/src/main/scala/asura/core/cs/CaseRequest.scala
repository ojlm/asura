package asura.core.cs

import asura.core.es.model.KeyValueObject

case class CaseRequest(
                        method: String,
                        url: String,
                        headers: Seq[KeyValueObject],
                        body: String
                      ) {

}
