package asura.ui.message;

import asura.ui.message.wd.HttpRequest;
import asura.ui.message.wd.HttpResponse;

public class IndigoMessage {

  public int type;
  public int id;
  public HttpRequest req;
  public HttpResponse res;

  public IndigoMessage() {
  }

  public IndigoMessage(int type) {
    this.type = type;
  }

  public IndigoMessage withId(int id) {
    this.id = id;
    return this;
  }

  public static IndigoMessage UNKNOWN = new IndigoMessage(IndigoMessageType.UNKNOWN);

  public static IndigoMessage ofRequest(int id, HttpRequest req) {
    IndigoMessage message = new IndigoMessage(IndigoMessageType.WD_REQ);
    message.id = id;
    message.req = req;
    return message;
  }

  public static IndigoMessage ofRequest(HttpRequest req) {
    return ofRequest(0, req);
  }

  public static IndigoMessage ofResponse(int id, HttpResponse res) {
    IndigoMessage message = new IndigoMessage(IndigoMessageType.WD_RES);
    message.id = id;
    message.res = res;
    return message;
  }

  public static IndigoMessage ofResponse(HttpResponse res) {
    return ofResponse(0, res);
  }

}
