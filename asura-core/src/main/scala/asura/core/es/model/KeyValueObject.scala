package asura.core.es.model

case class KeyValueObject(
                           val key: String,
                           val value: String,
                           enabled: Boolean = true,
                         ) {

}
