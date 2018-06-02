package asura.core.es.model

/**
  * [[asura.core.http.HttpContentTypes.JSON]] => "object/map json string"
  * [[asura.core.http.HttpContentTypes.TEXT_PLAIN]] => "plain text"
  * [[asura.core.http.HttpContentTypes.X_WWW_FORM_URLENCODED]] => "Seq[[asura.core.es.model.KeyValueObject]] json string"
  */
case class MediaObject(
                        contentType: String,
                        data: String
                      ) {

}
