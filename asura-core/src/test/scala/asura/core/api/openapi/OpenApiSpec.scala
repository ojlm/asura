package asura.core.api.openapi

import asura.common.ScalaTestBaseSpec
import asura.core.util.JacksonSupport
import io.swagger.models.parameters.BodyParameter
import io.swagger.parser.SwaggerParser

class OpenApiSpec extends ScalaTestBaseSpec {

  test("v2 parser") {
    val swagger = new SwaggerParser().read("http://petstore.swagger.io/v2/swagger.json")
    val parameters = swagger.getPath("/pet").getPost.getParameters
    val body = parameters.get(0).asInstanceOf[BodyParameter]
    assertResult(raw""""#/definitions/Pet"""")(JacksonSupport.stringify(body.getSchema.getReference))
  }
}
