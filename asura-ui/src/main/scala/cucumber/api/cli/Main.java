package cucumber.api.cli;

/**
 * replaces cucumber-jvm code
 */
public class Main {

  public static void main(String[] args) {
    loadOverride();
    com.intuit.karate.cli.IdeMain.main(args);
    System.exit(0);
  }

  public static void loadOverride() {
    com.intuit.karate.driver.chrome.Chrome.loadOverride();
    com.intuit.karate.ScenarioActions.loadOverride();
    com.intuit.karate.core.ScenarioEngine.loadOverride();
  }

}
