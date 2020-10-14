package asura.core.es.model

case class KeyValueObject(
                           key: String,
                           value: String,
                           enabled: Boolean = true,
                           description: String = null,
                         )
