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

  Scenario: IMG CROP
    * def ele = img.crop(0.5, 0.5).crop(0.5, 0.5, 0.2, 0.4).crop('0.8')
    * ele.screenshot()
    * print ele.ocrExtract()
