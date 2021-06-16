package asura.ui.message;

import asura.ui.message.wd.HttpRequest;
import asura.ui.message.wd.HttpResponse;

public class IndigoMessage {

  public int type;
  public HttpRequest req;
  public HttpResponse res;

  public IndigoMessage() {
  }

  public IndigoMessage(int type) {
    this.type = type;
  }

  public static IndigoMessage UNKNOWN = new IndigoMessage(IndigoMessageType.UNKNOWN);

  public static IndigoMessage ofRequest(HttpRequest req) {
    IndigoMessage message = new IndigoMessage(IndigoMessageType.WD_REQ);
    message.req = req;
    return message;
  }

  public static IndigoMessage ofResponse(HttpResponse res) {
    IndigoMessage message = new IndigoMessage(IndigoMessageType.WD_RES);
    message.res = res;
    return message;
  }

}
