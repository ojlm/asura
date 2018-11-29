package asura.core.es.model

import com.fasterxml.jackson.annotation.JsonProperty

case class OnlineRequestLog(
                             @JsonProperty("verb")
                             val method: String, // http method
                             @JsonProperty("domain")
                             val domain: String,
                             @JsonProperty("uri")
                             val uri: String, // http url path
                             @JsonProperty("args")
                             val args: String, // http url query string with '?'
                             @JsonProperty("status")
                             val status: Int,
                           )

object OnlineRequestLog {

  val KEY_DOMAIN = "domain"
  val KEY_METHOD = "verb"
}
