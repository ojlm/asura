Feature: Feature Demo

  Scenario: Scenario Demo
    * configure driver = { type: 'chrome', start: true, 'userDataDir': 'logs/chrome'}
    * driver 'https://github.com/'
    * delay(2000)
    * def targetUrl = 'https://github.com/search?q=asura+language:scala'
    * driver targetUrl
    * screenshot()
    * delay(2000)
