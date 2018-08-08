package asura.app.api.actions

import asura.app.api.BaseApi
import asura.common.model.ApiResError
import org.pac4j.core.context.HttpConstants
import org.pac4j.play.PlayWebContext
import org.pac4j.play.http.DefaultHttpActionAdapter
import play.mvc.Result

class SecurityHttpActionAdapter extends DefaultHttpActionAdapter {

  override def adapt(code: Int, context: PlayWebContext): Result = {
    if (code == HttpConstants.UNAUTHORIZED) {
      BaseApi.OkApiRes(ApiResError("UNAUTHORIZED")).asJava
    } else if (code == HttpConstants.FORBIDDEN) {
      BaseApi.OkApiRes(ApiResError("FORBIDDEN")).asJava
    } else {
      super.adapt(code, context)
    }
  }
}
