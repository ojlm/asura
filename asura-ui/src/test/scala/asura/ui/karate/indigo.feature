Feature: indigo

  Background:
    * configure driver = { type: 'indigo' }
    * driver ''

  Scenario: BasicGetInformation
    * screenshot()
    * print status()
    * print sessions()
    * print session()
    * print orientation()
    * print rotation()
    * print deviceInfo()
    * print deviceSize()
    * print devicePixelRatio()
    * print systemBars()
    * print battery()
    * print settings()
    # * print alertText()
    * print source()
    * screenshot('android.widget.FrameLayout')
    * print location('android.widget.FrameLayout')
    * print position('android.widget.FrameLayout')
    * print firstVisibleView('android.widget.FrameLayout')
    * print attribute('android.widget.FrameLayout', 'checked')
    * print size('android.widget.FrameLayout')
    * print name('android.widget.FrameLayout')
    * print driver.elementIds('android.widget.FrameLayout')
    * print driver.getDimensions()

  Scenario: BasicPost
    * tap(0.5, 0.5)
    * tap(0.5, 0.5)
    * back()
    * swipe(0,0,300,800,2)
    * longclick(0.5, 0.5, 1000)
    * notification()
    * back()
    * drag(100, 100, 600, 600, 1)
    * flick(100, 100)
    * clipboard('hello')
    * print clipboard()

  Scenario: Gestures
    * gestures.drag(100, 100, 600, 600)
    * gestures.fling(600, 600, 100, 100, 'up')
    * gestures.longClick(500, 500)
    * gestures.doubleClick(500, 500)
    * gestures.pinchClose(100, 100, 600, 600, 0.5)
    * gestures.scroll(100, 100, 600, 600, 'up', 0.5)
    * gestures.swipe(100, 100, 600, 600, 'down', 0.5)

  Scenario: Network
    * network()

  Scenario: rotation&&orientation
    * rotation(90)
    * orientation('portrait')
    * orientation('landscape')

  Scenario: CROP
    * img.crop().detect()
    * img.crop().click()
    * img.crop(0.5).screenshot()

