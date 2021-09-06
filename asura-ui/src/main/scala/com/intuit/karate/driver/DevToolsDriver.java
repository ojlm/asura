/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.driver;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.graalvm.polyglot.Value;

import com.intuit.karate.Constants;
import com.intuit.karate.FileUtils;
import com.intuit.karate.Json;
import com.intuit.karate.JsonUtils;
import com.intuit.karate.Logger;
import com.intuit.karate.StringUtils;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.MockHandler;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.Variable;
import com.intuit.karate.graal.JsValue;
import com.intuit.karate.http.HttpRequest;
import com.intuit.karate.http.ResourceType;
import com.intuit.karate.http.Response;
import com.intuit.karate.http.WebSocketClient;
import com.intuit.karate.http.WebSocketOptions;
import com.intuit.karate.shell.Command;

/**
 * @author pthomas3
 */
public abstract class DevToolsDriver implements Driver {

  protected final DriverOptions options;
  protected final Command command;
  protected WebSocketOptions wsOptions;
  protected WebSocketClient client;
  private boolean terminated;

  private final DevToolsWait wait;
  protected final String rootFrameId;

  private Integer windowId;
  private String windowState;
  private Integer executionContextId;
  protected String sessionId;

  protected boolean domContentEventFired;
  protected final Set<String> framesStillLoading = new HashSet();
  private final Map<String, String> frameSessions = new HashMap();
  private boolean submit;

  protected String currentDialogText;

  private int nextId;

  public int nextId() {
    return ++nextId;
  }

  private MockHandler mockHandler;

  protected final Logger logger;

  protected DevToolsDriver(DriverOptions options, Command command, String webSocketUrl) {
    logger = options.driverLogger;
    this.options = options;
    this.command = command;
    this.wait = new DevToolsWait(this, options);
    int pos = webSocketUrl.lastIndexOf('/');
    rootFrameId = webSocketUrl.substring(pos + 1);
    logger.debug("root frame id: {}", rootFrameId);
    wsOptions = new WebSocketOptions(webSocketUrl);
    wsOptions.setMaxPayloadSize(options.maxPayloadSize);
    wsOptions.setTextConsumer(text -> {
      if (logger.isTraceEnabled()) {
        logger.trace("<< {}", text);
      } else {
        // to avoid swamping the console when large base64 encoded binary responses happen
        logger.debug("<< {}", StringUtils.truncate(text, 1024, true));
      }
      Map<String, Object> map = Json.of(text).value();
      DevToolsMessage dtm = new DevToolsMessage(this, map);
      receive(dtm);
    });
    client = new WebSocketClient(wsOptions, logger);
  }

  public void reconnect(String url) {
    if (client != null) {
      client.close();
    }
    WebSocketOptions newWsOptions = new WebSocketOptions(url);
    newWsOptions.setMaxPayloadSize(wsOptions.getMaxPayloadSize());
    newWsOptions.setTextHandler(wsOptions.getTextHandler());
    wsOptions = newWsOptions;
    client = new WebSocketClient(wsOptions, logger);
  }

  @Override
  public Driver timeout(Integer millis) {
    options.setTimeout(millis);
    return this;
  }

  @Override
  public Driver timeout() {
    return timeout(null);
  }

  public DevToolsMessage method(String method) {
    return new DevToolsMessage(this, method);
  }

  // this can be used for exploring / sending any raw message !
  public Map<String, Object> send(Map<String, Object> map) {
    DevToolsMessage dtm = new DevToolsMessage(this, map);
    dtm.setId(nextId());
    return sendAndWait(dtm, null).toMap();
  }

  public void send(DevToolsMessage dtm) {
    String json = JsonUtils.toJson(dtm.toMap());
    logger.debug(">> {}", json);
    client.send(json);
  }

  public DevToolsMessage sendAndWait(DevToolsMessage dtm, Predicate<DevToolsMessage> condition) {
    boolean wasSubmit = submit;
    if (condition == null && submit) {
      submit = false;
      condition = DevToolsWait.ALL_FRAMES_LOADED;
    }
    // do stuff inside wait to avoid missing messages
    DevToolsMessage result = wait.send(dtm, condition);
    if (result == null && !wasSubmit) {
      throw new RuntimeException("failed to get reply for: " + dtm);
    }
    return result;
  }

  public void receive(DevToolsMessage dtm) {
    if (dtm.methodIs("Page.domContentEventFired")) {
      domContentEventFired = true;
      logger.trace("** set dom ready flag to true");
    }
    if (dtm.methodIs("Page.javascriptDialogOpening")) {
      currentDialogText = dtm.getParam("message");
      // this will stop waiting NOW
      wait.setCondition(DevToolsWait.DIALOG_OPENING);
    }
    if (dtm.methodIs("Page.frameStartedLoading")) {
      String frameLoadingId = dtm.getParam("frameId");
      if (rootFrameId.equals(frameLoadingId)) { // root page is loading
        domContentEventFired = false;
        framesStillLoading.clear();
        frameSessions.clear();
        logger.trace("** root frame started loading, cleared all page state: {}", frameLoadingId);
      } else {
        framesStillLoading.add(frameLoadingId);
        logger.trace("** frame started loading, added to in-progress list: {}", framesStillLoading);
      }
    }
    if (dtm.methodIs("Page.frameStoppedLoading")) {
      String frameLoadedId = dtm.getParam("frameId");
      framesStillLoading.remove(frameLoadedId);
      logger.trace("** frame stopped loading: {}, remaining in-progress: {}", frameLoadedId, framesStillLoading);
    }
    if (dtm.methodIs("Target.attachedToTarget")) {
      frameSessions.put(dtm.getParam("targetInfo.targetId"), dtm.getParam("sessionId"));
      logger.trace("** added frame session: {}", frameSessions);
    }
    if (dtm.methodIs("Fetch.requestPaused")) {
      handleInterceptedRequest(dtm);
    }
    // all needed state is set above before we get into conditional checks
    wait.receive(dtm);
  }

  private void handleInterceptedRequest(DevToolsMessage dtm) {
    String requestId = dtm.getParam("requestId");
    String requestUrl = dtm.getParam("request.url");
    if (mockHandler != null) {
      String method = dtm.getParam("request.method");
      Map<String, String> headers = dtm.getParam("request.headers");
      String postData = dtm.getParam("request.postData");
      logger.debug("intercepting request for url: {}", requestUrl);
      HttpRequest request = new HttpRequest();
      request.setUrl(requestUrl);
      request.setMethod(method);
      headers.forEach((k, v) -> request.putHeader(k, v));
      if (postData != null) {
        request.setBody(FileUtils.toBytes(postData));
      } else {
        request.setBody(Constants.ZERO_BYTES);
      }
      Response response = mockHandler.handle(request.toRequest());
      String responseBody = response.getBody() == null ? "" : Base64.getEncoder().encodeToString(response.getBody());
      List<Map> responseHeaders = new ArrayList();
      Map<String, List<String>> map = response.getHeaders();
      if (map != null) {
        map.forEach((k, v) -> {
          if (v != null && !v.isEmpty()) {
            Map<String, Object> nv = new HashMap(2);
            nv.put("name", k);
            nv.put("value", v.get(0));
            responseHeaders.add(nv);
          }
        });
      }
      method("Fetch.fulfillRequest")
        .param("requestId", requestId)
        .param("responseCode", response.getStatus())
        .param("responseHeaders", responseHeaders)
        .param("body", responseBody).sendWithoutWaiting();
    } else {
      logger.warn("no mock server, continuing paused request to url: {}", requestUrl);
      method("Fetch.continueRequest").param("requestId", requestId).sendWithoutWaiting();
    }
  }

  //==========================================================================
  //
  private DevToolsMessage evalOnce(String expression, boolean quickly, boolean fireAndForget) {
    DevToolsMessage toSend = method("Runtime.evaluate")
      .param("expression", expression).param("returnByValue", true);
    if (executionContextId != null) {
      toSend.param("contextId", executionContextId);
    }
    if (quickly) {
      toSend.setTimeout(options.getRetryInterval());
    }
    if (fireAndForget) {
      toSend.sendWithoutWaiting();
      return null;
    }
    return toSend.send();
  }

  protected DevToolsMessage eval(String expression) {
    return eval(expression, false);
  }

  private DevToolsMessage eval(String expression, boolean quickly) {
    DevToolsMessage dtm = evalOnce(expression, quickly, false);
    if (dtm.isResultError()) {
      String message = "js eval failed once:" + expression
        + ", error: " + dtm.getResult().getAsString();
      logger.warn(message);
      options.sleep();
      dtm = evalOnce(expression, quickly, false); // no wait condition for the re-try
      if (dtm.isResultError()) {
        message = "js eval failed twice:" + expression
          + ", error: " + dtm.getResult().getAsString();
        logger.error(message);
        throw new RuntimeException(message);
      }
    }
    return dtm;
  }

  protected void retryIfEnabled(String locator) {
    if (options.isRetryEnabled()) {
      waitFor(locator); // will throw exception if not found
    }
    if (options.highlight) {
      // highlight(locator, options.highlightDuration); // instead of this
      String highlightJs = options.highlight(locator, options.highlightDuration);
      evalOnce(highlightJs, true, true); // do it safely, i.e. fire and forget
    }
  }

  protected int getRootNodeId() {
    return method("DOM.getDocument").param("depth", 0).send().getResult("root.nodeId", Integer.class);
  }

  @Override
  public Integer elementId(String locator) {
    DevToolsMessage dtm = method("DOM.querySelector")
      .param("nodeId", getRootNodeId())
      .param("selector", locator).send();
    if (dtm.isResultError()) {
      return null;
    }
    return dtm.getResult("nodeId").getAsInt();
  }

  @Override
  public List elementIds(String locator) {
    if (locator.startsWith("/")) { // special handling for xpath
      getRootNodeId(); // just so that DOM.getDocument is called else DOM.performSearch fails
      DevToolsMessage dtm = method("DOM.performSearch").param("query", locator).send();
      String searchId = dtm.getResult("searchId", String.class);
      int resultCount = dtm.getResult("resultCount", Integer.class);
      dtm = method("DOM.getSearchResults")
        .param("searchId", searchId)
        .param("fromIndex", 0).param("toIndex", resultCount).send();
      return dtm.getResult("nodeIds", List.class);
    }
    DevToolsMessage dtm = method("DOM.querySelectorAll")
      .param("nodeId", getRootNodeId())
      .param("selector", locator).send();
    if (dtm.isResultError()) {
      return Collections.EMPTY_LIST;
    }
    return dtm.getResult("nodeIds").getValue();
  }

  @Override
  public DriverOptions getOptions() {
    return options;
  }

  @Override
  public void activate() {
    method("Target.activateTarget").param("targetId", rootFrameId).send();
  }

  protected void initWindowIdAndState() {
    DevToolsMessage dtm = method("Browser.getWindowForTarget").param("targetId", rootFrameId).send();
    if (!dtm.isResultError()) {
      windowId = dtm.getResult("windowId").getValue();
      windowState = (String) dtm.getResult("bounds").<Map> getValue().get("windowState");
    }
  }

  @Override
  public Map<String, Object> getDimensions() {
    DevToolsMessage dtm = method("Browser.getWindowForTarget").param("targetId", rootFrameId).send();
    Map<String, Object> map = dtm.getResult("bounds").getValue();
    Integer x = (Integer) map.remove("left");
    Integer y = (Integer) map.remove("top");
    map.put("x", x);
    map.put("y", y);
    return map;
  }

  @Override
  public void setDimensions(Map<String, Object> map) {
    Integer left = (Integer) map.remove("x");
    Integer top = (Integer) map.remove("y");
    map.put("left", left);
    map.put("top", top);
    Map temp = getDimensions();
    temp.putAll(map);
    temp.remove("windowState");
    method("Browser.setWindowBounds")
      .param("windowId", windowId)
      .param("bounds", temp).send();
  }

  public void emulateDevice(int width, int height, String userAgent) {
    logger.info("Setting deviceMetrics width={}, height={}, userAgent={}", width, height, userAgent);
    method("Network.setUserAgentOverride").param("userAgent", userAgent).send();
    method("Emulation.setDeviceMetricsOverride")
      .param("width", width)
      .param("height", height)
      .param("deviceScaleFactor", 1)
      .param("mobile", true)
      .send();
  }

  @Override
  public void close() {
    method("Page.close").sendWithoutWaiting();
  }

  @Override
  public void quit() {
    if (terminated) {
      return;
    }
    terminated = true;
    // don't wait, may fail and hang
    method("Target.closeTarget").param("targetId", rootFrameId).sendWithoutWaiting();
    // method("Browser.close").send();
    client.close();
    if (command != null) {
      command.close(true);
    }
  }

  @Override
  public boolean isTerminated() {
    return terminated;
  }

  @Override
  public void setUrl(String url) {
    method("Page.navigate").param("url", url)
      .send(DevToolsWait.ALL_FRAMES_LOADED);
  }

  @Override
  public void refresh() {
    method("Page.reload").send(DevToolsWait.ALL_FRAMES_LOADED);
  }

  @Override
  public void reload() {
    method("Page.reload").param("ignoreCache", true).send();
  }

  private void history(int delta) {
    DevToolsMessage dtm = method("Page.getNavigationHistory").send();
    int currentIndex = dtm.getResult("currentIndex").getValue();
    List<Map> list = dtm.getResult("entries").getValue();
    int targetIndex = currentIndex + delta;
    if (targetIndex < 0 || targetIndex == list.size()) {
      return;
    }
    Map<String, Object> entry = list.get(targetIndex);
    Integer id = (Integer) entry.get("id");
    method("Page.navigateToHistoryEntry").param("entryId", id).send(DevToolsWait.ALL_FRAMES_LOADED);
  }

  @Override
  public void back() {
    history(-1);
  }

  @Override
  public void forward() {
    history(1);
  }

  private void setWindowState(String state) {
    if (!"normal".equals(windowState)) {
      method("Browser.setWindowBounds")
        .param("windowId", windowId)
        .param("bounds", Collections.singletonMap("windowState", "normal"))
        .send();
      windowState = "normal";
    }
    if (!state.equals(windowState)) {
      method("Browser.setWindowBounds")
        .param("windowId", windowId)
        .param("bounds", Collections.singletonMap("windowState", state))
        .send();
      windowState = state;
    }
  }

  @Override
  public void maximize() {
    setWindowState("maximized");
  }

  @Override
  public void minimize() {
    setWindowState("minimized");
  }

  @Override
  public void fullscreen() {
    setWindowState("fullscreen");
  }

  @Override
  public Element click(String locator) {
    retryIfEnabled(locator);
    eval(DriverOptions.selector(locator) + ".click()");
    return DriverElement.locatorExists(this, locator);
  }

  @Override
  public Element select(String locator, String text) {
    retryIfEnabled(locator);
    eval(options.optionSelector(locator, text));
    return DriverElement.locatorExists(this, locator);
  }

  @Override
  public Element select(String locator, int index) {
    retryIfEnabled(locator);
    eval(options.optionSelector(locator, index));
    return DriverElement.locatorExists(this, locator);
  }

  @Override
  public Driver submit() {
    submit = true;
    return this;
  }

  @Override
  public Element focus(String locator) {
    retryIfEnabled(locator);
    eval(options.focusJs(locator));
    return DriverElement.locatorExists(this, locator);
  }

  @Override
  public Element clear(String locator) {
    eval(DriverOptions.selector(locator) + ".value = ''");
    return DriverElement.locatorExists(this, locator);
  }

  private void sendKey(char c, int modifiers, String type, Integer keyCode) {
    DevToolsMessage dtm = method("Input.dispatchKeyEvent")
      .param("modifiers", modifiers)
      .param("type", type);
    if (keyCode == null) {
      dtm.param("text", c + "");
    } else {
      switch (keyCode) {
        case 13:
          dtm.param("text", "\r"); // important ! \n does NOT work for chrome
          break;
        case 9: // TAB
          if ("char".equals(type)) {
            return; // special case
          }
          dtm.param("text", "");
          break;
        case 46: // DOT
          if ("rawKeyDown".equals(type)) {
            dtm.param("type", "keyDown"); // special case
          }
          dtm.param("text", ".");
          break;
        default:
          dtm.param("text", c + "");
      }
      dtm.param("windowsVirtualKeyCode", keyCode);
    }
    dtm.send();
  }

  @Override
  public Element input(String locator, String value) {
    retryIfEnabled(locator);
    // focus
    eval(options.focusJs(locator));
    Input input = new Input(value);
    while (input.hasNext()) {
      char c = input.next();
      int modifiers = input.getModifierFlags();
      Integer keyCode = Keys.code(c);
      if (keyCode != null) {
        sendKey(c, modifiers, "rawKeyDown", keyCode);
        sendKey(c, modifiers, "char", keyCode);
        sendKey(c, modifiers, "keyUp", keyCode);
      } else {
        logger.warn("unknown character / key code: {}", c);
        sendKey(c, modifiers, "char", null);
      }
    }
    return DriverElement.locatorExists(this, locator);
  }

  protected int currentMouseXpos;
  protected int currentMouseYpos;

  @Override
  public void actions(List<Map<String, Object>> sequence) {
    boolean submitRequested = submit;
    submit = false; // make sure only LAST action is handled as a submit()
    for (Map<String, Object> map : sequence) {
      List<Map<String, Object>> actions = (List) map.get("actions");
      if (actions == null) {
        logger.warn("no actions property found: {}", sequence);
        return;
      }
      Iterator<Map<String, Object>> iterator = actions.iterator();
      while (iterator.hasNext()) {
        Map<String, Object> action = iterator.next();
        String type = (String) action.get("type");
        if (type == null) {
          logger.warn("no type property found: {}", action);
          continue;
        }
        String chromeType;
        switch (type) {
          case "pointerMove":
            chromeType = "mouseMoved";
            break;
          case "pointerDown":
            chromeType = "mousePressed";
            break;
          case "pointerUp":
            chromeType = "mouseReleased";
            break;
          default:
            logger.warn("unexpected action type: {}", action);
            continue;
        }
        Integer x = (Integer) action.get("x");
        Integer y = (Integer) action.get("y");
        if (x != null) {
          currentMouseXpos = x;
        }
        if (y != null) {
          currentMouseYpos = y;
        }
        Integer duration = (Integer) action.get("duration");
        DevToolsMessage toSend = method("Input.dispatchMouseEvent")
          .param("type", chromeType)
          .param("x", currentMouseXpos).param("y", currentMouseYpos);
        if ("mousePressed".equals(chromeType) || "mouseReleased".equals(chromeType)) {
          toSend.param("button", "left").param("clickCount", 1);
        }
        if (!iterator.hasNext() && submitRequested) {
          submit = true;
        }
        toSend.send();
        if (duration != null) {
          options.sleep(duration);
        }
      }
    }
  }

  @Override
  public String text(String id) {
    return property(id, "textContent");
  }

  @Override
  public String html(String id) {
    return property(id, "outerHTML");
  }

  @Override
  public String value(String locator) {
    return property(locator, "value");
  }

  @Override
  public Element value(String locator, String value) {
    retryIfEnabled(locator);
    eval(DriverOptions.selector(locator) + ".value = '" + value + "'");
    return DriverElement.locatorExists(this, locator);
  }

  @Override
  public String attribute(String id, String name) {
    retryIfEnabled(id);
    DevToolsMessage dtm = eval(DriverOptions.selector(id) + ".getAttribute('" + name + "')");
    return dtm.getResult().getAsString();
  }

  @Override
  public String property(String id, String name) {
    retryIfEnabled(id);
    DevToolsMessage dtm = eval(DriverOptions.selector(id) + "['" + name + "']");
    return dtm.getResult().getAsString();
  }

  @Override
  public boolean enabled(String id) {
    retryIfEnabled(id);
    DevToolsMessage dtm = eval(DriverOptions.selector(id) + ".disabled");
    return !dtm.getResult().isTrue();
  }

  @Override
  public boolean waitUntil(String expression) {
    return options.retry(() -> {
      try {
        return eval(expression, true).getResult().isTrue();
      } catch (Exception e) {
        logger.warn("waitUntil evaluate failed: {}", e.getMessage());
        return false;
      }
    }, b -> b, "waitUntil (js)", true);
  }

  @Override
  public Object script(String expression) {
    return eval(expression).getResult().getValue();
  }

  @Override
  public String getTitle() {
    return eval("document.title").getResult().getAsString();
  }

  @Override
  public String getUrl() {
    return eval("document.location.href").getResult().getAsString();
  }

  @Override
  public List<Map> getCookies() {
    DevToolsMessage dtm = method("Network.getAllCookies").send();
    return dtm.getResult("cookies").getValue();
  }

  @Override
  public Map<String, Object> cookie(String name) {
    List<Map> list = getCookies();
    if (list == null) {
      return null;
    }
    for (Map<String, Object> map : list) {
      if (map != null && name.equals(map.get("name"))) {
        return map;
      }
    }
    return null;
  }

  @Override
  public void cookie(Map<String, Object> cookie) {
    if (cookie.get("url") == null && cookie.get("domain") == null) {
      cookie = new HashMap(cookie); // don't mutate test
      cookie.put("url", getUrl());
    }
    method("Network.setCookie").params(cookie).send();
  }

  @Override
  public void deleteCookie(String name) {
    method("Network.deleteCookies").param("name", name).param("url", getUrl()).send();
  }

  @Override
  public void clearCookies() {
    method("Network.clearBrowserCookies").send();
  }

  @Override
  public void dialog(boolean accept) {
    dialog(accept, null);
  }

  @Override
  public void dialog(boolean accept, String text) {
    DevToolsMessage temp = method("Page.handleJavaScriptDialog").param("accept", accept);
    if (text == null) {
      temp.send();
    } else {
      temp.param("promptText", text).send();
    }
  }

  @Override
  public String getDialogText() {
    return currentDialogText;
  }

  @Override
  public byte[] pdf(Map<String, Object> options) {
    DevToolsMessage dtm = method("Page.printToPDF").params(options).send();
    String temp = dtm.getResult("data").getAsString();
    return Base64.getDecoder().decode(temp);
  }

  @Override
  public byte[] screenshot(boolean embed) {
    return screenshot(null, embed);
  }

  @Override
  public Map<String, Object> position(String locator) {
    boolean submitTemp = submit; // in case we are prepping for a submit().mouse(locator).click()
    submit = false;
    retryIfEnabled(locator);
    Map<String, Object> map = eval(DriverOptions.getPositionJs(locator)).getResult().getValue();
    submit = submitTemp;
    return map;
  }

  @Override
  public byte[] screenshot(String id, boolean embed) {
    DevToolsMessage dtm;
    if (id == null) {
      dtm = method("Page.captureScreenshot").send();
    } else {
      Map<String, Object> map = position(id);
      map.put("scale", 1);
      dtm = method("Page.captureScreenshot").param("clip", map).send();
    }
    String temp = dtm.getResult("data").getAsString();
    byte[] bytes = Base64.getDecoder().decode(temp);
    if (embed) {
      getRuntime().embed(bytes, ResourceType.PNG);
    }
    return bytes;
  }

  // chrome only
  public byte[] screenshotFull() {
    DevToolsMessage layout = method("Page.getLayoutMetrics").send();
    Map<String, Object> size = layout.getResult("contentSize").getValue();
    Map<String, Object> map = options.newMapWithSelectedKeys(size, "height", "width");
    map.put("x", 0);
    map.put("y", 0);
    map.put("scale", 1);
    DevToolsMessage dtm = method("Page.captureScreenshot").param("clip", map).send();
    if (dtm.isResultError()) {
      logger.error("unable to capture screenshot: {}", dtm);
      return new byte[0];
    }
    String temp = dtm.getResult("data").getAsString();
    return Base64.getDecoder().decode(temp);
  }

  @Override
  public List<String> getPages() {
    DevToolsMessage dtm = method("Target.getTargets").send();
    return dtm.getResult("targetInfos.targetId").getValue();
  }

  @Override
  public void switchPage(String titleOrUrl) {
    if (titleOrUrl == null) {
      return;
    }
    DevToolsMessage dtm = method("Target.getTargets").send();
    List<Map> targets = dtm.getResult("targetInfos").getValue();
    String targetId = null;
    for (Map map : targets) {
      String title = (String) map.get("title");
      String url = (String) map.get("url");
      if ((title != null && title.contains(titleOrUrl))
        || (url != null && url.contains(titleOrUrl))) {
        targetId = (String) map.get("targetId");
        break;
      }
    }
    if (targetId != null) {
      method("Target.activateTarget").param("targetId", targetId).send();
    } else {
      logger.warn("failed to switch page to {}", titleOrUrl);
    }
  }

  @Override
  public void switchPage(int index) {
    if (index == -1) {
      return;
    }
    DevToolsMessage dtm = method("Target.getTargets").send();
    List<Map> targets = dtm.getResult("targetInfos").getValue();
    if (index < targets.size()) {
      Map target = targets.get(index);
      String targetId = (String) target.get("targetId");
      method("Target.activateTarget").param("targetId", targetId).send();
    } else {
      logger.warn("failed to switch frame by index: {}", index);
    }
  }

  @Override
  public void switchFrame(int index) {
    if (index == -1) {
      executionContextId = null;
      sessionId = null;
      return;
    }
    List<Integer> ids = elementIds("iframe,frame");
    if (index < ids.size()) {
      Integer nodeId = ids.get(index);
      setExecutionContext(nodeId, index);
    } else {
      logger.warn("unable to switch frame by index: {}", index);
    }
  }

  @Override
  public void switchFrame(String locator) {
    if (locator == null) {
      executionContextId = null;
      sessionId = null;
      return;
    }
    retryIfEnabled(locator);
    Integer nodeId = elementId(locator);
    if (nodeId == null) {
      return;
    }
    setExecutionContext(nodeId, locator);
  }

  private void setExecutionContext(int nodeId, Object locator) {
    DevToolsMessage dtm = method("DOM.describeNode")
      .param("nodeId", nodeId)
      .param("depth", 0)
      .send();
    String frameId = dtm.getResult("node.frameId", String.class);
    if (frameId == null) {
      logger.warn("unable to find frame by nodeId: {}", locator);
      return;
    }
    sessionId = frameSessions.get(frameId);
    if (sessionId != null) {
      logger.trace("found out-of-process frame - session: {} - {}", frameId, sessionId);
      return;
    }
    dtm = method("Page.createIsolatedWorld").param("frameId", frameId).send();
    executionContextId = dtm.getResult("executionContextId").getValue();
    if (executionContextId == null) {
      logger.warn("execution context is null, unable to switch frame by locator: {}", locator);
    }
  }

  public void enableNetworkEvents() {
    method("Network.enable").send();
  }

  public void enablePageEvents() {
    method("Page.enable").send();
  }

  public void enableRuntimeEvents() {
    method("Runtime.enable").send();
  }

  public void enableTargetEvents() {
    method("Target.setAutoAttach")
      .param("autoAttach", true)
      .param("waitForDebuggerOnStart", false)
      .param("flatten", true).send();
  }

  public void intercept(Value value) {
    Map<String, Object> config = (Map) JsValue.toJava(value);
    config = new Variable(config).getValue();
    intercept(config);
  }

  public void intercept(Map<String, Object> config) {
    List<String> patterns = (List) config.get("patterns");
    if (patterns == null) {
      throw new RuntimeException("missing array argument 'patterns': " + config);
    }
    if (mockHandler != null) {
      throw new RuntimeException("'intercept()' can be called only once");
    }
    String mock = (String) config.get("mock");
    if (mock == null) {
      throw new RuntimeException("missing argument 'mock': " + config);
    }
    Object o = getRuntime().engine.fileReader.readFile(mock);
    if (!(o instanceof Feature)) {
      throw new RuntimeException("'mock' is not a feature file: " + config + ", " + mock);
    }
    Feature feature = (Feature) o;
    mockHandler = new MockHandler(feature);
    method("Fetch.enable").param("patterns", patterns).send();
  }

  public void inputFile(String locator, String... relativePaths) {
    List<String> files = new ArrayList(relativePaths.length);
    ScenarioEngine engine = ScenarioEngine.get();
    for (String p : relativePaths) {
      files.add(engine.fileReader.toAbsolutePath(p));
    }
    Integer nodeId = elementId(locator);
    method("DOM.setFileInputFiles").param("files", files).param("nodeId", nodeId).send();
  }

  public Object scriptAwait(String expression) {
    DevToolsMessage toSend = method("Runtime.evaluate")
      .param("expression", expression)
      .param("returnByValue", true)
      .param("awaitPromise", true);
    if (executionContextId != null) {
      toSend.param("contextId", executionContextId);
    }
    return toSend.send().getResult().getValue();
  }

}
