package asura.core.api.openapi

case class ConvertOptions(
                           scheme: String,
                           host: String,
                           port: Int,
                           basePath: String,
                           labels: Seq[String] = Nil,
                         )
