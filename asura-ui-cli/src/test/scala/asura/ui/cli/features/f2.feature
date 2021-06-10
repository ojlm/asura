Feature: Feature.f2

  @f2 @s1
  Scenario: f2.s1
    * configure driver = { type: 'chrome', start: true, stop: true, 'userDataDir': 'logs/chrome'}
    * driver 'https://github.com/'
    * delay(500)
    * screenshot()
    * def targetUrl = 'https://github.com/team'
    * driver targetUrl
    * screenshot()
    * 打开 'https://github.com/marketplace' 网页
    * screenshot()
    * delay(500)
