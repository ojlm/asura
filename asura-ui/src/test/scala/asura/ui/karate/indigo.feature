Feature: indigo

  Scenario: Basic
    * configure driver = { type: 'indigo', start: false}
    * driver ''
    * print status()
    * print source()
    * screenshot()
    * print attribute('android.widget.FrameLayout', 'checked')
    * print driver.elementIds('android.widget.FrameLayout')
    * print driver.getDimensions()
    * img.crop().detect()
