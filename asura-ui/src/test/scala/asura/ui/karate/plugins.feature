Feature: Plugins

  Background:
    * configure driver = { type: 'chrome', start: false, stop: false, 'userDataDir': 'logs/chrome'}
    * driver 'https://www.baidu.com/'

  Scenario: OCR extract
    * print ocr.extract()

  Scenario: OCR click
    * ocr.click('#s-top-left', '视频')

  Scenario: IMG click
    * img.click('#lg', 'baidu.png')

  Scenario: IMG DIFF
    * print img.diff('#lg', 'baidu.png')
    * print img.diff('#s_lg_img', 'baidu.png')
    * print img.compare('#s_lg_img', 'baidu.png')

