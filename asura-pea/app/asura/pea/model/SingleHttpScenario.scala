package asura.pea.model

case class SingleHttpScenario(
                               var name: String,
                               var request: SingleRequest,
                               var injection: Injection,
                             )
