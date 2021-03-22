Feature: Feature Demo

  Scenario: Scenario Demo
    * configure driver = { type: 'chrome', start: false, stop: false, 'userDataDir': 'logs/chrome'}
    * driver 'https://github.com/'
    * delay(2000)
    * def targetUrl = 'https://github.com/search?q=asura+language:scala'
    * driver targetUrl
    * screenshot()
    * 打开 'https://github.com/' 网页
    * delay(2000)
