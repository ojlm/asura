package asura.play.actions

import asura.common.model.{ApiCode, ApiRes}
import asura.play.api.BaseApi
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.exception.http.HttpAction
import org.pac4j.play.PlayWebContext
import org.pac4j.play.http.PlayHttpActionAdapter
import play.mvc.Result

class SecurityHttpActionAdapter extends PlayHttpActionAdapter {

  override def adapt(action: HttpAction, context: PlayWebContext): Result = {
    val code = action.getCode
    if (code == HttpConstants.UNAUTHORIZED) {
      BaseApi.OkApiRes(ApiRes(ApiCode.NOT_LOGIN, "UNAUTHORIZED")).asJava
    } else if (code == HttpConstants.FORBIDDEN) {
      BaseApi.OkApiRes(ApiRes(ApiCode.PERMISSION_DENIED, "FORBIDDEN")).asJava
    } else {
      super.adapt(action, context)
    }
  }
}
