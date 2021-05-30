Feature: Feature Demo

  Scenario: Switch Page Demo
    * configure driver = { type: 'chrome', start: false, stop: false, 'userDataDir': 'logs/chrome'}
    * driver 'https://github.com/'
    * print html('body')
    * open new 'https://www.baidu.com/'
    * print html('body')
    * switch page 'baidu'
    * print html('body')
    * goto top
    * print html('body')
    * driver.close()
