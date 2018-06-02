package asura.core.es.model

/**
  * job data structure
  *
  * @param cs       case id  list
  * @param scenario scenario id list
  * @param ext      external data for extension
  */
case class JobData(
                    cs: Seq[DocRef] = Nil,
                    scenario: Seq[DocRef] = Nil,
                    ext: Map[String, Any] = Map.empty,
                  ) {
}
