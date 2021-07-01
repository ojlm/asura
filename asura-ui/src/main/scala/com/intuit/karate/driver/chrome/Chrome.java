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
package com.intuit.karate.driver.chrome;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intuit.karate.FileUtils;
import com.intuit.karate.Http;
import com.intuit.karate.Json;
import com.intuit.karate.StringUtils;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.driver.DevToolsDriver;
import com.intuit.karate.driver.DevToolsMessage;
import com.intuit.karate.driver.DriverOptions;
import com.intuit.karate.driver.Input;
import com.intuit.karate.driver.Keys;
import com.intuit.karate.http.Response;
import com.intuit.karate.shell.Command;

import asura.ui.driver.DriverProvider;
import asura.ui.karate.KarateRunner;

/**
 * @author pthomas3
 */
public class Chrome extends DevToolsDriver {

  private static Logger logger = LoggerFactory.getLogger(Chrome.class);
  public static final String DEFAULT_PATH_MAC = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
  public static final String DEFAULT_PATH_WIN = "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe";
  public static final String DEFAULT_PATH_LINUX = "/usr/bin/google-chrome";

  public ScenarioEngine engine;
  public Boolean inject;
  public Consumer<Map<String, Object>> filter;

  public Chrome(DriverOptions options, Command command, String webSocketUrl) {
    super(options, command, webSocketUrl);
  }

  // 自定义
  public Chrome(DriverOptions options, Command command, String webSocketUrl,
    ScenarioEngine engine, Boolean inject, Consumer<Map<String, Object>> filter
  ) {
    super(options, command, webSocketUrl);
    this.engine = engine;
    this.inject = inject;
    this.filter = filter;
    if (this.inject && this.engine != null) {
      this.engine.setDriver(this);
    }
    client.setTextHandler(text -> {
      Map<String, Object> map = Json.of(text).value();
      DevToolsMessage dtm = new DevToolsMessage(this, map);
      if (this.filter != null && !StringUtils.isBlank(dtm.getMethod())) {
        this.filter.accept(map);
      }
      receive(dtm);
      return false; // no async signalling, for normal use, e.g. chrome developer tools
    });
  }

  public void closeClient() {
    client.close();
  }

  public void enableLog() {
    method("Log.enable").send();
  }

  public void enableDom() {
    method("DOM.enable").send();
  }

  public void setDiscoverTargets() {
    method("Target.setDiscoverTargets").param("discover", true).send();
  }

  public String screenshotAsBase64() {
    return method("Page.captureScreenshot").send().getResult("data").getAsString();
  }

  public DevToolsMessage openNewPage(String url) {
    return method("Target.createTarget")
      .param("url", url)
      .param("newWindow", false)
      .param("background", true)
      .send();
  }

  public void sendKey(char c, int modifiers, String type, Integer keyCode) {
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

  public void input(String value) {
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
        sendKey(c, modifiers, "char", -1);
      }
    }
  }

  public static void loadOverride() {
    logger.info("use override chrome");
  }

  @Override
  public void quit() {
    DriverProvider provider = DriverOptions.getDriverProvider();
    if (provider != null) {
      provider.release(this);
    } else {
      super.quit();
    }
  }

  public static Chrome start(Map<String, Object> map, ScenarioRuntime sr) {
    DriverOptions options = new DriverOptions(map, sr, 9222,
      FileUtils.isOsWindows() ? DEFAULT_PATH_WIN : FileUtils.isOsMacOsX() ? DEFAULT_PATH_MAC : DEFAULT_PATH_LINUX);
    options.arg("--remote-debugging-port=" + options.port);
    options.arg("--no-first-run");
    options.arg("--disable-translate");
    options.arg("--disable-notifications");
    options.arg("--disable-infobars");
    options.arg("--disable-gpu");
    options.arg("--dbus-stub");
    options.arg("--disable-dev-shm-usage");
    if (options.userDataDir != null) {
      options.arg("--user-data-dir=" + options.userDataDir);
    }
    options.arg("--disable-popup-blocking");
    if (options.headless) {
      options.arg("--headless");
    }
    Command command = options.startProcess();
    String webSocketUrl = null;
    if (map.containsKey("debuggerUrl")) {
      webSocketUrl = (String) map.get("debuggerUrl");
    } else {
      Object targetId = map.get("targetId");
      Object startUrl = map.get("startUrl");
      Object top = map.get("top");
      Http http = options.getHttp();
      Command.waitForHttp(http.urlBase + "/json");
      Response res = http.path("json").get();
      if (res.json().asList().isEmpty()) {
        if (command != null) {
          command.close(true);
        }
        throw new RuntimeException("chrome server returned empty list from " + http.urlBase);
      }
      List<Map<String, Object>> targets = res.json().asList();
      for (Map<String, Object> target : targets) {
        String targetUrl = (String) target.get("url");
        if (targetUrl == null || targetUrl.startsWith("chrome-")) {
          continue;
        }
        if (top != null && top.equals(true)) {
          webSocketUrl = (String) target.get("webSocketDebuggerUrl");
          break;
        } else if (targetId != null) {
          if (targetId.equals(target.get("id"))) {
            webSocketUrl = (String) target.get("webSocketDebuggerUrl");
            break;
          }
        } else if (startUrl != null) {
          String targetTitle = (String) target.get("title");
          if (targetUrl.contains(startUrl.toString()) || targetTitle.contains(startUrl.toString())) {
            webSocketUrl = (String) target.get("webSocketDebuggerUrl");
            break;
          }
        } else {
          String targetType = (String) target.get("type");
          if (!"page".equals(targetType)) {
            continue;
          }
          webSocketUrl = (String) target.get("webSocketDebuggerUrl");
          if (options.attach == null) { // take the first
            break;
          }
          if (targetUrl.contains(options.attach)) {
            break;
          }
        }
      }
    }
    if (webSocketUrl == null) {
      throw new RuntimeException("failed to attach to chrome debug server");
    }
    Boolean inject = (Boolean) map.getOrDefault("_inject", false);
    Consumer<Map<String, Object>> filter = (Consumer<Map<String, Object>>) map.getOrDefault("_filter", null);
    Chrome chrome = new Chrome(options, command, webSocketUrl, sr.engine, inject, filter);
    chrome.activate();
    chrome.enablePageEvents();
    chrome.enableRuntimeEvents();
    chrome.enableTargetEvents();
    chrome.enableLog();
    chrome.setDiscoverTargets();
    if (!options.headless) {
      chrome.initWindowIdAndState();
    }
    return chrome;
  }

  public static Chrome start(Boolean start, Consumer<Map<String, Object>> filter, Boolean inject) {
    Map<String, Object> options = new HashMap();
    options.put("start", start);
    options.put("_inject", inject);
    options.put("_filter", filter);
    return Chrome.start(options, KarateRunner.buildScenarioEngine().runtime);
  }

  public static Chrome start(Map<String, Object> options, Consumer<Map<String, Object>> filter, Boolean inject) {
    options.put("_inject", inject);
    options.put("_filter", filter);
    return Chrome.start(options, KarateRunner.buildScenarioEngine().runtime);
  }

  public static Chrome start(Map<String, Object> options, ScenarioEngine engine, Consumer<Map<String, Object>> filter,
    Boolean inject) {
    options.put("_inject", inject);
    options.put("_filter", filter);
    return Chrome.start(options, engine.runtime);
  }

  public static Chrome start(String chromeExecutablePath, boolean headless) {
    Map<String, Object> options = new HashMap();
    options.put("executable", chromeExecutablePath);
    options.put("headless", headless);
    return Chrome.start(options, null);
  }

  public static Chrome start(Map<String, Object> options) {
    if (options == null) {
      options = new HashMap();
    }
    return Chrome.start(options, null);
  }

  public static Chrome start() {
    return start(null);
  }

  public static Chrome startHeadless() {
    return start(Collections.singletonMap("headless", true));
  }

}
