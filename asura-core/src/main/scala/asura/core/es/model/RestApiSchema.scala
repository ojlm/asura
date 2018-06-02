package asura.core.es.model

case class RestApiSchema(
                          path: Seq[ParameterSchema],
                          query: Seq[ParameterSchema],
                          header: Seq[ParameterSchema],
                          cookie: Seq[ParameterSchema],
                          requestBody: HttpRequestBody,

                          /**
                            * the key of `response` is `default` or `http status code`
                            */
                          responses: Map[String, HttpResponse],
                        ) extends ApiSchema {

}
