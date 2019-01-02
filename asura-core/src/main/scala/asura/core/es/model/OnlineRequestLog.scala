package asura.core.es.model

case class OnlineRequestLog(
                             val method: String,
                             val domain: String,
                             val uri: String,
                             val args: String, // http url query string with '?'
                             val status: Int,
                           )
