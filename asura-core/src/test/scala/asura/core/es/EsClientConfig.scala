package asura.core.es

trait EsClientConfig {

  val url = "elasticsearch://localhost:9200?cluster.name=asura"
  EsClient.init(url)
}
