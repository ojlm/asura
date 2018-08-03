package asura.app.api

import javax.inject.Singleton

@Singleton
class SwaggerApi extends BaseApi {

  def home() = Action {
    Redirect("/docs/swagger-ui/index.html?url=/assets/swagger.json", MOVED_PERMANENTLY)
  }
}
