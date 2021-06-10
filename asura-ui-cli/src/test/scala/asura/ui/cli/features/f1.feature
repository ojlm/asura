Feature: Feature.f1

  @f1 @s1
  Scenario: f1.s1
    * configure driver = { type: 'chrome', start: true, stop: true, 'userDataDir': 'logs/chrome'}
    * driver 'https://www.kuaishou.com/'
    * delay(500)
    * screenshot()
