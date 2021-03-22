@report=false
Feature: Multiple Driver Feature

  Background:
    * configure newDriver = { type: 'chrome', name: 'd01', default: true, start: true, stop: true, port: 9222}
    * configure newDriver = { type: 'chrome', name: 'd02', start: true, stop: true, port: 9223}

  Scenario: Multiple Driver Scenario
    * driver 'https://github.com/'
    * delay(500)
    * def targetUrl = 'https://github.com/search?q=asura+language:scala'
    * driver targetUrl
    * d01.setUrl('https://github.com/')
    * d02.setUrl('https://github.com/team')
    * delay(500)
    * use driver d02
    * driver 'https://github.com/marketplace'
    * delay(5000)
