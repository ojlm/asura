package asura.core.es.model

/**
  * `protocol`, `host`,`port` and `auth` here for situation when need to override fields in `env`.
  */
case class Request(
                    val protocol: String,
                    val host: String,
                    val port: Int,
                    val auth: Authorization,
                    var method: String,
                    var path: Seq[KeyValueObject],
                    var query: Seq[KeyValueObject],
                    var header: Seq[KeyValueObject],
                    var cookie: Seq[KeyValueObject],
                    var contentType: String,
                    var body: Seq[MediaObject],
                  ) {
}
