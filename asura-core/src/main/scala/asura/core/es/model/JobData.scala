package asura.core.es.model

import asura.core.es.model.JobData.JobDataExt

case class JobData(
                    cs: Seq[DocRef] = Nil,
                    scenario: Seq[ScenarioStep] = Nil,
                    ext: JobDataExt = null,
                  ) {
}

object JobData {

  case class JobDataExt(
                         path: String = null,
                         methods: Seq[String] = null,
                         text: String = null,
                         labels: Seq[String] = null,
                       )

}
