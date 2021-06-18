Feature: indigo

  Scenario: Android
    * configure driver = { type: 'indigo', serial: 'bb8695f8', start: false}
    * driver ''
    * print status()
    * print source()
    * screenshot()
    * print attribute('android.widget.FrameLayout', 'checked')
    * print driver.elementIds('android.widget.FrameLayout')
    * print driver.getDimensions()
