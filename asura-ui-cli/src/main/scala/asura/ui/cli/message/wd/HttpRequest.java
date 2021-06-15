package asura.ui.cli.message.wd;

import java.util.Map;

public class HttpRequest {

  /* "GET", "POST", "PUT" or "DELETE" */
  public String method;
  public String uri;
  public String body;
  public Iterable<Map.Entry<String, String>> headers;
  public Map<String, Object> data;

}
