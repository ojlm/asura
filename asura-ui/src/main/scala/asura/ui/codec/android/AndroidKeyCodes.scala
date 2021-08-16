package asura.ui.codec.android

// copied from <https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/view/KeyEvent.java>
// blob: 99d6c3463ef5369c153cc9c441fd47929e6b0915
object AndroidKeyCodes {

  /** Key code constant: Unknown key code. */
  val KEYCODE_UNKNOWN = 0
  /** Key code constant: Soft Left key.
   * Usually situated below the display on phones and used as a multi-function
   * feature key for selecting a software defined function shown on the bottom left
   * of the display. */
  val KEYCODE_SOFT_LEFT = 1
  /** Key code constant: Soft Right key.
   * Usually situated below the display on phones and used as a multi-function
   * feature key for selecting a software defined function shown on the bottom right
   * of the display. */
  val KEYCODE_SOFT_RIGHT = 2
  /** Key code constant: Home key.
   * This key is handled by the framework and is never delivered to applications. */
  val KEYCODE_HOME = 3
  /** Key code constant: Back key. */
  val KEYCODE_BACK = 4
  /** Key code constant: Call key. */
  val KEYCODE_CALL = 5
  /** Key code constant: End Call key. */
  val KEYCODE_ENDCALL = 6
  /** Key code constant: '0' key. */
  val KEYCODE_0 = 7
  /** Key code constant: '1' key. */
  val KEYCODE_1 = 8
  /** Key code constant: '2' key. */
  val KEYCODE_2 = 9
  /** Key code constant: '3' key. */
  val KEYCODE_3 = 10
  /** Key code constant: '4' key. */
  val KEYCODE_4 = 11
  /** Key code constant: '5' key. */
  val KEYCODE_5 = 12
  /** Key code constant: '6' key. */
  val KEYCODE_6 = 13
  /** Key code constant: '7' key. */
  val KEYCODE_7 = 14
  /** Key code constant: '8' key. */
  val KEYCODE_8 = 15
  /** Key code constant: '9' key. */
  val KEYCODE_9 = 16
  /** Key code constant: '*' key. */
  val KEYCODE_STAR = 17
  /** Key code constant: '#' key. */
  val KEYCODE_POUND = 18
  /** Key code constant: Directional Pad Up key.
   * May also be synthesized from trackball motions. */
  val KEYCODE_DPAD_UP = 19
  /** Key code constant: Directional Pad Down key.
   * May also be synthesized from trackball motions. */
  val KEYCODE_DPAD_DOWN = 20
  /** Key code constant: Directional Pad Left key.
   * May also be synthesized from trackball motions. */
  val KEYCODE_DPAD_LEFT = 21
  /** Key code constant: Directional Pad Right key.
   * May also be synthesized from trackball motions. */
  val KEYCODE_DPAD_RIGHT = 22
  /** Key code constant: Directional Pad Center key.
   * May also be synthesized from trackball motions. */
  val KEYCODE_DPAD_CENTER = 23
  /** Key code constant: Volume Up key.
   * Adjusts the speaker volume up. */
  val KEYCODE_VOLUME_UP = 24
  /** Key code constant: Volume Down key.
   * Adjusts the speaker volume down. */
  val KEYCODE_VOLUME_DOWN = 25
  /** Key code constant: Power key. */
  val KEYCODE_POWER = 26
  /** Key code constant: Camera key.
   * Used to launch a camera application or take pictures. */
  val KEYCODE_CAMERA = 27
  /** Key code constant: Clear key. */
  val KEYCODE_CLEAR = 28
  /** Key code constant: 'A' key. */
  val KEYCODE_A = 29
  /** Key code constant: 'B' key. */
  val KEYCODE_B = 30
  /** Key code constant: 'C' key. */
  val KEYCODE_C = 31
  /** Key code constant: 'D' key. */
  val KEYCODE_D = 32
  /** Key code constant: 'E' key. */
  val KEYCODE_E = 33
  /** Key code constant: 'F' key. */
  val KEYCODE_F = 34
  /** Key code constant: 'G' key. */
  val KEYCODE_G = 35
  /** Key code constant: 'H' key. */
  val KEYCODE_H = 36
  /** Key code constant: 'I' key. */
  val KEYCODE_I = 37
  /** Key code constant: 'J' key. */
  val KEYCODE_J = 38
  /** Key code constant: 'K' key. */
  val KEYCODE_K = 39
  /** Key code constant: 'L' key. */
  val KEYCODE_L = 40
  /** Key code constant: 'M' key. */
  val KEYCODE_M = 41
  /** Key code constant: 'N' key. */
  val KEYCODE_N = 42
  /** Key code constant: 'O' key. */
  val KEYCODE_O = 43
  /** Key code constant: 'P' key. */
  val KEYCODE_P = 44
  /** Key code constant: 'Q' key. */
  val KEYCODE_Q = 45
  /** Key code constant: 'R' key. */
  val KEYCODE_R = 46
  /** Key code constant: 'S' key. */
  val KEYCODE_S = 47
  /** Key code constant: 'T' key. */
  val KEYCODE_T = 48
  /** Key code constant: 'U' key. */
  val KEYCODE_U = 49
  /** Key code constant: 'V' key. */
  val KEYCODE_V = 50
  /** Key code constant: 'W' key. */
  val KEYCODE_W = 51
  /** Key code constant: 'X' key. */
  val KEYCODE_X = 52
  /** Key code constant: 'Y' key. */
  val KEYCODE_Y = 53
  /** Key code constant: 'Z' key. */
  val KEYCODE_Z = 54
  /** Key code constant: ',' key. */
  val KEYCODE_COMMA = 55
  /** Key code constant: '.' key. */
  val KEYCODE_PERIOD = 56
  /** Key code constant: Left Alt modifier key. */
  val KEYCODE_ALT_LEFT = 57
  /** Key code constant: Right Alt modifier key. */
  val KEYCODE_ALT_RIGHT = 58
  /** Key code constant: Left Shift modifier key. */
  val KEYCODE_SHIFT_LEFT = 59
  /** Key code constant: Right Shift modifier key. */
  val KEYCODE_SHIFT_RIGHT = 60
  /** Key code constant: Tab key. */
  val KEYCODE_TAB = 61
  /** Key code constant: Space key. */
  val KEYCODE_SPACE = 62
  /** Key code constant: Symbol modifier key.
   * Used to enter alternate symbols. */
  val KEYCODE_SYM = 63
  /** Key code constant: Explorer special function key.
   * Used to launch a browser application. */
  val KEYCODE_EXPLORER = 64
  /** Key code constant: Envelope special function key.
   * Used to launch a mail application. */
  val KEYCODE_ENVELOPE = 65
  /** Key code constant: Enter key. */
  val KEYCODE_ENTER = 66
  /** Key code constant: Backspace key.
   * Deletes characters before the insertion point, unlike {@link #KEYCODE_FORWARD_DEL}. */
  val KEYCODE_DEL = 67
  /** Key code constant: '`' (backtick) key. */
  val KEYCODE_GRAVE = 68
  /** Key code constant: '-'. */
  val KEYCODE_MINUS = 69
  /** Key code constant: '=' key. */
  val KEYCODE_EQUALS = 70
  /** Key code constant: '[' key. */
  val KEYCODE_LEFT_BRACKET = 71
  /** Key code constant: ']' key. */
  val KEYCODE_RIGHT_BRACKET = 72
  /** Key code constant: '\' key. */
  val KEYCODE_BACKSLASH = 73
  /** Key code constant: ';' key. */
  val KEYCODE_SEMICOLON = 74
  /** Key code constant: ''' (apostrophe) key. */
  val KEYCODE_APOSTROPHE = 75
  /** Key code constant: '/' key. */
  val KEYCODE_SLASH = 76
  /** Key code constant: '@' key. */
  val KEYCODE_AT = 77
  /** Key code constant: Number modifier key.
   * Used to enter numeric symbols.
   * This key is not Num Lock; it is more like {@link #KEYCODE_ALT_LEFT} and is
   * interpreted as an ALT key by {@link android.text.method.MetaKeyKeyListener}. */
  val KEYCODE_NUM = 78
  /** Key code constant: Headset Hook key.
   * Used to hang up calls and stop media. */
  val KEYCODE_HEADSETHOOK = 79
  /** Key code constant: Camera Focus key.
   * Used to focus the camera. */
  val KEYCODE_FOCUS = 80 // *Camera* focus

  /** Key code constant: '+' key. */
  val KEYCODE_PLUS = 81
  /** Key code constant: Menu key. */
  val KEYCODE_MENU = 82
  /** Key code constant: Notification key. */
  val KEYCODE_NOTIFICATION = 83
  /** Key code constant: Search key. */
  val KEYCODE_SEARCH = 84
  /** Key code constant: Play/Pause media key. */
  val KEYCODE_MEDIA_PLAY_PAUSE = 85
  /** Key code constant: Stop media key. */
  val KEYCODE_MEDIA_STOP = 86
  /** Key code constant: Play Next media key. */
  val KEYCODE_MEDIA_NEXT = 87
  /** Key code constant: Play Previous media key. */
  val KEYCODE_MEDIA_PREVIOUS = 88
  /** Key code constant: Rewind media key. */
  val KEYCODE_MEDIA_REWIND = 89
  /** Key code constant: Fast Forward media key. */
  val KEYCODE_MEDIA_FAST_FORWARD = 90
  /** Key code constant: Mute key.
   * Mutes the microphone, unlike {@link #KEYCODE_VOLUME_MUTE}. */
  val KEYCODE_MUTE = 91
  /** Key code constant: Page Up key. */
  val KEYCODE_PAGE_UP = 92
  /** Key code constant: Page Down key. */
  val KEYCODE_PAGE_DOWN = 93
  /** Key code constant: Picture Symbols modifier key.
   * Used to switch symbol sets (Emoji, Kao-moji). */
  val KEYCODE_PICTSYMBOLS = 94 // switch symbol-sets (Emoji,Kao-moji)

  /** Key code constant: Switch Charset modifier key.
   * Used to switch character sets (Kanji, Katakana). */
  val KEYCODE_SWITCH_CHARSET = 95 // switch char-sets (Kanji,Katakana)

  /** Key code constant: A Button key.
   * On a game controller, the A button should be either the button labeled A
   * or the first button on the bottom row of controller buttons. */
  val KEYCODE_BUTTON_A = 96
  /** Key code constant: B Button key.
   * On a game controller, the B button should be either the button labeled B
   * or the second button on the bottom row of controller buttons. */
  val KEYCODE_BUTTON_B = 97
  /** Key code constant: C Button key.
   * On a game controller, the C button should be either the button labeled C
   * or the third button on the bottom row of controller buttons. */
  val KEYCODE_BUTTON_C = 98
  /** Key code constant: X Button key.
   * On a game controller, the X button should be either the button labeled X
   * or the first button on the upper row of controller buttons. */
  val KEYCODE_BUTTON_X = 99
  /** Key code constant: Y Button key.
   * On a game controller, the Y button should be either the button labeled Y
   * or the second button on the upper row of controller buttons. */
  val KEYCODE_BUTTON_Y = 100
  /** Key code constant: Z Button key.
   * On a game controller, the Z button should be either the button labeled Z
   * or the third button on the upper row of controller buttons. */
  val KEYCODE_BUTTON_Z = 101
  /** Key code constant: L1 Button key.
   * On a game controller, the L1 button should be either the button labeled L1 (or L)
   * or the top left trigger button. */
  val KEYCODE_BUTTON_L1 = 102
  /** Key code constant: R1 Button key.
   * On a game controller, the R1 button should be either the button labeled R1 (or R)
   * or the top right trigger button. */
  val KEYCODE_BUTTON_R1 = 103
  /** Key code constant: L2 Button key.
   * On a game controller, the L2 button should be either the button labeled L2
   * or the bottom left trigger button. */
  val KEYCODE_BUTTON_L2 = 104
  /** Key code constant: R2 Button key.
   * On a game controller, the R2 button should be either the button labeled R2
   * or the bottom right trigger button. */
  val KEYCODE_BUTTON_R2 = 105
  /** Key code constant: Left Thumb Button key.
   * On a game controller, the left thumb button indicates that the left (or only)
   * joystick is pressed. */
  val KEYCODE_BUTTON_THUMBL = 106
  /** Key code constant: Right Thumb Button key.
   * On a game controller, the right thumb button indicates that the right
   * joystick is pressed. */
  val KEYCODE_BUTTON_THUMBR = 107
  /** Key code constant: Start Button key.
   * On a game controller, the button labeled Start. */
  val KEYCODE_BUTTON_START = 108
  /** Key code constant: Select Button key.
   * On a game controller, the button labeled Select. */
  val KEYCODE_BUTTON_SELECT = 109
  /** Key code constant: Mode Button key.
   * On a game controller, the button labeled Mode. */
  val KEYCODE_BUTTON_MODE = 110
  /** Key code constant: Escape key. */
  val KEYCODE_ESCAPE = 111
  /** Key code constant: Forward Delete key.
   * Deletes characters ahead of the insertion point, unlike {@link #KEYCODE_DEL}. */
  val KEYCODE_FORWARD_DEL = 112
  /** Key code constant: Left Control modifier key. */
  val KEYCODE_CTRL_LEFT = 113
  /** Key code constant: Right Control modifier key. */
  val KEYCODE_CTRL_RIGHT = 114
  /** Key code constant: Caps Lock key. */
  val KEYCODE_CAPS_LOCK = 115
  /** Key code constant: Scroll Lock key. */
  val KEYCODE_SCROLL_LOCK = 116
  /** Key code constant: Left Meta modifier key. */
  val KEYCODE_META_LEFT = 117
  /** Key code constant: Right Meta modifier key. */
  val KEYCODE_META_RIGHT = 118
  /** Key code constant: Function modifier key. */
  val KEYCODE_FUNCTION = 119
  /** Key code constant: System Request / Print Screen key. */
  val KEYCODE_SYSRQ = 120
  /** Key code constant: Break / Pause key. */
  val KEYCODE_BREAK = 121
  /** Key code constant: Home Movement key.
   * Used for scrolling or moving the cursor around to the start of a line
   * or to the top of a list. */
  val KEYCODE_MOVE_HOME = 122
  /** Key code constant: End Movement key.
   * Used for scrolling or moving the cursor around to the end of a line
   * or to the bottom of a list. */
  val KEYCODE_MOVE_END = 123
  /** Key code constant: Insert key.
   * Toggles insert / overwrite edit mode. */
  val KEYCODE_INSERT = 124
  /** Key code constant: Forward key.
   * Navigates forward in the history stack.  Complement of {@link #KEYCODE_BACK}. */
  val KEYCODE_FORWARD = 125
  /** Key code constant: Play media key. */
  val KEYCODE_MEDIA_PLAY = 126
  /** Key code constant: Pause media key. */
  val KEYCODE_MEDIA_PAUSE = 127
  /** Key code constant: Close media key.
   * May be used to close a CD tray, for example. */
  val KEYCODE_MEDIA_CLOSE = 128
  /** Key code constant: Eject media key.
   * May be used to eject a CD tray, for example. */
  val KEYCODE_MEDIA_EJECT = 129
  /** Key code constant: Record media key. */
  val KEYCODE_MEDIA_RECORD = 130
  /** Key code constant: F1 key. */
  val KEYCODE_F1 = 131
  /** Key code constant: F2 key. */
  val KEYCODE_F2 = 132
  /** Key code constant: F3 key. */
  val KEYCODE_F3 = 133
  /** Key code constant: F4 key. */
  val KEYCODE_F4 = 134
  /** Key code constant: F5 key. */
  val KEYCODE_F5 = 135
  /** Key code constant: F6 key. */
  val KEYCODE_F6 = 136
  /** Key code constant: F7 key. */
  val KEYCODE_F7 = 137
  /** Key code constant: F8 key. */
  val KEYCODE_F8 = 138
  /** Key code constant: F9 key. */
  val KEYCODE_F9 = 139
  /** Key code constant: F10 key. */
  val KEYCODE_F10 = 140
  /** Key code constant: F11 key. */
  val KEYCODE_F11 = 141
  /** Key code constant: F12 key. */
  val KEYCODE_F12 = 142
  /** Key code constant: Num Lock key.
   * This is the Num Lock key; it is different from {@link #KEYCODE_NUM}.
   * This key alters the behavior of other keys on the numeric keypad. */
  val KEYCODE_NUM_LOCK = 143
  /** Key code constant: Numeric keypad '0' key. */
  val KEYCODE_NUMPAD_0 = 144
  /** Key code constant: Numeric keypad '1' key. */
  val KEYCODE_NUMPAD_1 = 145
  /** Key code constant: Numeric keypad '2' key. */
  val KEYCODE_NUMPAD_2 = 146
  /** Key code constant: Numeric keypad '3' key. */
  val KEYCODE_NUMPAD_3 = 147
  /** Key code constant: Numeric keypad '4' key. */
  val KEYCODE_NUMPAD_4 = 148
  /** Key code constant: Numeric keypad '5' key. */
  val KEYCODE_NUMPAD_5 = 149
  /** Key code constant: Numeric keypad '6' key. */
  val KEYCODE_NUMPAD_6 = 150
  /** Key code constant: Numeric keypad '7' key. */
  val KEYCODE_NUMPAD_7 = 151
  /** Key code constant: Numeric keypad '8' key. */
  val KEYCODE_NUMPAD_8 = 152
  /** Key code constant: Numeric keypad '9' key. */
  val KEYCODE_NUMPAD_9 = 153
  /** Key code constant: Numeric keypad '/' key (for division). */
  val KEYCODE_NUMPAD_DIVIDE = 154
  /** Key code constant: Numeric keypad '*' key (for multiplication). */
  val KEYCODE_NUMPAD_MULTIPLY = 155
  /** Key code constant: Numeric keypad '-' key (for subtraction). */
  val KEYCODE_NUMPAD_SUBTRACT = 156
  /** Key code constant: Numeric keypad '+' key (for addition). */
  val KEYCODE_NUMPAD_ADD = 157
  /** Key code constant: Numeric keypad '.' key (for decimals or digit grouping). */
  val KEYCODE_NUMPAD_DOT = 158
  /** Key code constant: Numeric keypad ',' key (for decimals or digit grouping). */
  val KEYCODE_NUMPAD_COMMA = 159
  /** Key code constant: Numeric keypad Enter key. */
  val KEYCODE_NUMPAD_ENTER = 160
  /** Key code constant: Numeric keypad '=' key. */
  val KEYCODE_NUMPAD_EQUALS = 161
  /** Key code constant: Numeric keypad '(' key. */
  val KEYCODE_NUMPAD_LEFT_PAREN = 162
  /** Key code constant: Numeric keypad ')' key. */
  val KEYCODE_NUMPAD_RIGHT_PAREN = 163
  /** Key code constant: Volume Mute key.
   * Mutes the speaker, unlike {@link #KEYCODE_MUTE}.
   * This key should normally be implemented as a toggle such that the first press
   * mutes the speaker and the second press restores the original volume. */
  val KEYCODE_VOLUME_MUTE = 164
  /** Key code constant: Info key.
   * Common on TV remotes to show additional information related to what is
   * currently being viewed. */
  val KEYCODE_INFO = 165
  /** Key code constant: Channel up key.
   * On TV remotes, increments the television channel. */
  val KEYCODE_CHANNEL_UP = 166
  /** Key code constant: Channel down key.
   * On TV remotes, decrements the television channel. */
  val KEYCODE_CHANNEL_DOWN = 167
  /** Key code constant: Zoom in key. */
  val KEYCODE_ZOOM_IN = 168
  /** Key code constant: Zoom out key. */
  val KEYCODE_ZOOM_OUT = 169
  /** Key code constant: TV key.
   * On TV remotes, switches to viewing live TV. */
  val KEYCODE_TV = 170
  /** Key code constant: Window key.
   * On TV remotes, toggles picture-in-picture mode or other windowing functions.
   * On Android Wear devices, triggers a display offset. */
  val KEYCODE_WINDOW = 171
  /** Key code constant: Guide key.
   * On TV remotes, shows a programming guide. */
  val KEYCODE_GUIDE = 172
  /** Key code constant: DVR key.
   * On some TV remotes, switches to a DVR mode for recorded shows. */
  val KEYCODE_DVR = 173
  /** Key code constant: Bookmark key.
   * On some TV remotes, bookmarks content or web pages. */
  val KEYCODE_BOOKMARK = 174
  /** Key code constant: Toggle captions key.
   * Switches the mode for closed-captioning text, for example during television shows. */
  val KEYCODE_CAPTIONS = 175
  /** Key code constant: Settings key.
   * Starts the system settings activity. */
  val KEYCODE_SETTINGS = 176
  /** Key code constant: TV power key.
   * On TV remotes, toggles the power on a television screen. */
  val KEYCODE_TV_POWER = 177
  /** Key code constant: TV input key.
   * On TV remotes, switches the input on a television screen. */
  val KEYCODE_TV_INPUT = 178
  /** Key code constant: Set-top-box power key.
   * On TV remotes, toggles the power on an external Set-top-box. */
  val KEYCODE_STB_POWER = 179
  /** Key code constant: Set-top-box input key.
   * On TV remotes, switches the input mode on an external Set-top-box. */
  val KEYCODE_STB_INPUT = 180
  /** Key code constant: A/V Receiver power key.
   * On TV remotes, toggles the power on an external A/V Receiver. */
  val KEYCODE_AVR_POWER = 181
  /** Key code constant: A/V Receiver input key.
   * On TV remotes, switches the input mode on an external A/V Receiver. */
  val KEYCODE_AVR_INPUT = 182
  /** Key code constant: Red "programmable" key.
   * On TV remotes, acts as a contextual/programmable key. */
  val KEYCODE_PROG_RED = 183
  /** Key code constant: Green "programmable" key.
   * On TV remotes, actsas a contextual/programmable key. */
  val KEYCODE_PROG_GREEN = 184
  /** Key code constant: Yellow "programmable" key.
   * On TV remotes, acts as a contextual/programmable key. */
  val KEYCODE_PROG_YELLOW = 185
  /** Key code constant: Blue "programmable" key.
   * On TV remotes, acts as a contextual/programmable key. */
  val KEYCODE_PROG_BLUE = 186
  /** Key code constant: App switch key.
   * Should bring up the application switcher dialog. */
  val KEYCODE_APP_SWITCH = 187
  /** Key code constant: Generic Game Pad Button #1. */
  val KEYCODE_BUTTON_1 = 188
  /** Key code constant: Generic Game Pad Button #2. */
  val KEYCODE_BUTTON_2 = 189
  /** Key code constant: Generic Game Pad Button #3. */
  val KEYCODE_BUTTON_3 = 190
  /** Key code constant: Generic Game Pad Button #4. */
  val KEYCODE_BUTTON_4 = 191
  /** Key code constant: Generic Game Pad Button #5. */
  val KEYCODE_BUTTON_5 = 192
  /** Key code constant: Generic Game Pad Button #6. */
  val KEYCODE_BUTTON_6 = 193
  /** Key code constant: Generic Game Pad Button #7. */
  val KEYCODE_BUTTON_7 = 194
  /** Key code constant: Generic Game Pad Button #8. */
  val KEYCODE_BUTTON_8 = 195
  /** Key code constant: Generic Game Pad Button #9. */
  val KEYCODE_BUTTON_9 = 196
  /** Key code constant: Generic Game Pad Button #10. */
  val KEYCODE_BUTTON_10 = 197
  /** Key code constant: Generic Game Pad Button #11. */
  val KEYCODE_BUTTON_11 = 198
  /** Key code constant: Generic Game Pad Button #12. */
  val KEYCODE_BUTTON_12 = 199
  /** Key code constant: Generic Game Pad Button #13. */
  val KEYCODE_BUTTON_13 = 200
  /** Key code constant: Generic Game Pad Button #14. */
  val KEYCODE_BUTTON_14 = 201
  /** Key code constant: Generic Game Pad Button #15. */
  val KEYCODE_BUTTON_15 = 202
  /** Key code constant: Generic Game Pad Button #16. */
  val KEYCODE_BUTTON_16 = 203
  /** Key code constant: Language Switch key.
   * Toggles the current input language such as switching between English and Japanese on
   * a QWERTY keyboard.  On some devices, the same function may be performed by
   * pressing Shift+Spacebar. */
  val KEYCODE_LANGUAGE_SWITCH = 204
  /** Key code constant: Manner Mode key.
   * Toggles silent or vibrate mode on and off to make the device behave more politely
   * in certain settings such as on a crowded train.  On some devices, the key may only
   * operate when long-pressed. */
  val KEYCODE_MANNER_MODE = 205
  /** Key code constant: 3D Mode key.
   * Toggles the display between 2D and 3D mode. */
  val KEYCODE_3D_MODE = 206
  /** Key code constant: Contacts special function key.
   * Used to launch an address book application. */
  val KEYCODE_CONTACTS = 207
  /** Key code constant: Calendar special function key.
   * Used to launch a calendar application. */
  val KEYCODE_CALENDAR = 208
  /** Key code constant: Music special function key.
   * Used to launch a music player application. */
  val KEYCODE_MUSIC = 209
  /** Key code constant: Calculator special function key.
   * Used to launch a calculator application. */
  val KEYCODE_CALCULATOR = 210
  /** Key code constant: Japanese full-width / half-width key. */
  val KEYCODE_ZENKAKU_HANKAKU = 211
  /** Key code constant: Japanese alphanumeric key. */
  val KEYCODE_EISU = 212
  /** Key code constant: Japanese non-conversion key. */
  val KEYCODE_MUHENKAN = 213
  /** Key code constant: Japanese conversion key. */
  val KEYCODE_HENKAN = 214
  /** Key code constant: Japanese katakana / hiragana key. */
  val KEYCODE_KATAKANA_HIRAGANA = 215
  /** Key code constant: Japanese Yen key. */
  val KEYCODE_YEN = 216
  /** Key code constant: Japanese Ro key. */
  val KEYCODE_RO = 217
  /** Key code constant: Japanese kana key. */
  val KEYCODE_KANA = 218
  /** Key code constant: Assist key.
   * Launches the global assist activity.  Not delivered to applications. */
  val KEYCODE_ASSIST = 219
  /** Key code constant: Brightness Down key.
   * Adjusts the screen brightness down. */
  val KEYCODE_BRIGHTNESS_DOWN = 220
  /** Key code constant: Brightness Up key.
   * Adjusts the screen brightness up. */
  val KEYCODE_BRIGHTNESS_UP = 221
  /** Key code constant: Audio Track key.
   * Switches the audio tracks. */
  val KEYCODE_MEDIA_AUDIO_TRACK = 222
  /** Key code constant: Sleep key.
   * Puts the device to sleep.  Behaves somewhat like {@link #KEYCODE_POWER} but it
   * has no effect if the device is already asleep. */
  val KEYCODE_SLEEP = 223
  /** Key code constant: Wakeup key.
   * Wakes up the device.  Behaves somewhat like {@link #KEYCODE_POWER} but it
   * has no effect if the device is already awake. */
  val KEYCODE_WAKEUP = 224
  /** Key code constant: Pairing key.
   * Initiates peripheral pairing mode. Useful for pairing remote control
   * devices or game controllers, especially if no other input mode is
   * available. */
  val KEYCODE_PAIRING = 225
  /** Key code constant: Media Top Menu key.
   * Goes to the top of media menu. */
  val KEYCODE_MEDIA_TOP_MENU = 226
  /** Key code constant: '11' key. */
  val KEYCODE_11 = 227
  /** Key code constant: '12' key. */
  val KEYCODE_12 = 228
  /** Key code constant: Last Channel key.
   * Goes to the last viewed channel. */
  val KEYCODE_LAST_CHANNEL = 229
  /** Key code constant: TV data service key.
   * Displays data services like weather, sports. */
  val KEYCODE_TV_DATA_SERVICE = 230
  /** Key code constant: Voice Assist key.
   * Launches the global voice assist activity. Not delivered to applications. */
  val KEYCODE_VOICE_ASSIST = 231
  /** Key code constant: Radio key.
   * Toggles TV service / Radio service. */
  val KEYCODE_TV_RADIO_SERVICE = 232
  /** Key code constant: Teletext key.
   * Displays Teletext service. */
  val KEYCODE_TV_TELETEXT = 233
  /** Key code constant: Number entry key.
   * Initiates to enter multi-digit channel nubmber when each digit key is assigned
   * for selecting separate channel. Corresponds to Number Entry Mode (0x1D) of CEC
   * User Control Code. */
  val KEYCODE_TV_NUMBER_ENTRY = 234
  /** Key code constant: Analog Terrestrial key.
   * Switches to analog terrestrial broadcast service. */
  val KEYCODE_TV_TERRESTRIAL_ANALOG = 235
  /** Key code constant: Digital Terrestrial key.
   * Switches to digital terrestrial broadcast service. */
  val KEYCODE_TV_TERRESTRIAL_DIGITAL = 236
  /** Key code constant: Satellite key.
   * Switches to digital satellite broadcast service. */
  val KEYCODE_TV_SATELLITE = 237
  /** Key code constant: BS key.
   * Switches to BS digital satellite broadcasting service available in Japan. */
  val KEYCODE_TV_SATELLITE_BS = 238
  /** Key code constant: CS key.
   * Switches to CS digital satellite broadcasting service available in Japan. */
  val KEYCODE_TV_SATELLITE_CS = 239
  /** Key code constant: BS/CS key.
   * Toggles between BS and CS digital satellite services. */
  val KEYCODE_TV_SATELLITE_SERVICE = 240
  /** Key code constant: Toggle Network key.
   * Toggles selecting broacast services. */
  val KEYCODE_TV_NETWORK = 241
  /** Key code constant: Antenna/Cable key.
   * Toggles broadcast input source between antenna and cable. */
  val KEYCODE_TV_ANTENNA_CABLE = 242
  /** Key code constant: HDMI #1 key.
   * Switches to HDMI input #1. */
  val KEYCODE_TV_INPUT_HDMI_1 = 243
  /** Key code constant: HDMI #2 key.
   * Switches to HDMI input #2. */
  val KEYCODE_TV_INPUT_HDMI_2 = 244
  /** Key code constant: HDMI #3 key.
   * Switches to HDMI input #3. */
  val KEYCODE_TV_INPUT_HDMI_3 = 245
  /** Key code constant: HDMI #4 key.
   * Switches to HDMI input #4. */
  val KEYCODE_TV_INPUT_HDMI_4 = 246
  /** Key code constant: Composite #1 key.
   * Switches to composite video input #1. */
  val KEYCODE_TV_INPUT_COMPOSITE_1 = 247
  /** Key code constant: Composite #2 key.
   * Switches to composite video input #2. */
  val KEYCODE_TV_INPUT_COMPOSITE_2 = 248
  /** Key code constant: Component #1 key.
   * Switches to component video input #1. */
  val KEYCODE_TV_INPUT_COMPONENT_1 = 249
  /** Key code constant: Component #2 key.
   * Switches to component video input #2. */
  val KEYCODE_TV_INPUT_COMPONENT_2 = 250
  /** Key code constant: VGA #1 key.
   * Switches to VGA (analog RGB) input #1. */
  val KEYCODE_TV_INPUT_VGA_1 = 251
  /** Key code constant: Audio description key.
   * Toggles audio description off / on. */
  val KEYCODE_TV_AUDIO_DESCRIPTION = 252
  /** Key code constant: Audio description mixing volume up key.
   * Louden audio description volume as compared with normal audio volume. */
  val KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP = 253
  /** Key code constant: Audio description mixing volume down key.
   * Lessen audio description volume as compared with normal audio volume. */
  val KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN = 254
  /** Key code constant: Zoom mode key.
   * Changes Zoom mode (Normal, Full, Zoom, Wide-zoom, etc.) */
  val KEYCODE_TV_ZOOM_MODE = 255
  /** Key code constant: Contents menu key.
   * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control
   * Code */
  val KEYCODE_TV_CONTENTS_MENU = 256
  /** Key code constant: Media context menu key.
   * Goes to the context menu of media contents. Corresponds to Media Context-sensitive
   * Menu (0x11) of CEC User Control Code. */
  val KEYCODE_TV_MEDIA_CONTEXT_MENU = 257
  /** Key code constant: Timer programming key.
   * Goes to the timer recording menu. Corresponds to Timer Programming (0x54) of
   * CEC User Control Code. */
  val KEYCODE_TV_TIMER_PROGRAMMING = 258
  /** Key code constant: Help key. */
  val KEYCODE_HELP = 259
  /** Key code constant: Navigate to previous key.
   * Goes backward by one item in an ordered collection of items. */
  val KEYCODE_NAVIGATE_PREVIOUS = 260
  /** Key code constant: Navigate to next key.
   * Advances to the next item in an ordered collection of items. */
  val KEYCODE_NAVIGATE_NEXT = 261
  /** Key code constant: Navigate in key.
   * Activates the item that currently has focus or expands to the next level of a navigation
   * hierarchy. */
  val KEYCODE_NAVIGATE_IN = 262
  /** Key code constant: Navigate out key.
   * Backs out one level of a navigation hierarchy or collapses the item that currently has
   * focus. */
  val KEYCODE_NAVIGATE_OUT = 263
  /** Key code constant: Primary stem key for Wear
   * Main power/reset button on watch. */
  val KEYCODE_STEM_PRIMARY = 264
  /** Key code constant: Generic stem key 1 for Wear */
  val KEYCODE_STEM_1 = 265
  /** Key code constant: Generic stem key 2 for Wear */
  val KEYCODE_STEM_2 = 266
  /** Key code constant: Generic stem key 3 for Wear */
  val KEYCODE_STEM_3 = 267
  /** Key code constant: Directional Pad Up-Left */
  val KEYCODE_DPAD_UP_LEFT = 268
  /** Key code constant: Directional Pad Down-Left */
  val KEYCODE_DPAD_DOWN_LEFT = 269
  /** Key code constant: Directional Pad Up-Right */
  val KEYCODE_DPAD_UP_RIGHT = 270
  /** Key code constant: Directional Pad Down-Right */
  val KEYCODE_DPAD_DOWN_RIGHT = 271
  /** Key code constant: Skip forward media key. */
  val KEYCODE_MEDIA_SKIP_FORWARD = 272
  /** Key code constant: Skip backward media key. */
  val KEYCODE_MEDIA_SKIP_BACKWARD = 273
  /** Key code constant: Step forward media key.
   * Steps media forward, one frame at a time. */
  val KEYCODE_MEDIA_STEP_FORWARD = 274
  /** Key code constant: Step backward media key.
   * Steps media backward, one frame at a time. */
  val KEYCODE_MEDIA_STEP_BACKWARD = 275
  /** Key code constant: put device to sleep unless a wakelock is held. */
  val KEYCODE_SOFT_SLEEP = 276
  /** Key code constant: Cut key. */
  val KEYCODE_CUT = 277
  /** Key code constant: Copy key. */
  val KEYCODE_COPY = 278
  /** Key code constant: Paste key. */
  val KEYCODE_PASTE = 279
  /** Key code constant: Consumed by the system for navigation up */
  val KEYCODE_SYSTEM_NAVIGATION_UP = 280
  /** Key code constant: Consumed by the system for navigation down */
  val KEYCODE_SYSTEM_NAVIGATION_DOWN = 281
  /** Key code constant: Consumed by the system for navigation left */
  val KEYCODE_SYSTEM_NAVIGATION_LEFT = 282
  /** Key code constant: Consumed by the system for navigation right */
  val KEYCODE_SYSTEM_NAVIGATION_RIGHT = 283
  /** Key code constant: Show all apps */
  val KEYCODE_ALL_APPS = 284
  /** Key code constant: Refresh key. */
  val KEYCODE_REFRESH = 285
  /** Key code constant: Thumbs up key. Apps can use this to let user upvote content. */
  val KEYCODE_THUMBS_UP = 286
  /** Key code constant: Thumbs down key. Apps can use this to let user downvote content. */
  val KEYCODE_THUMBS_DOWN = 287
  /**
   * Key code constant: Used to switch current {@link android.accounts.Account} that is
   * consuming content. May be consumed by system to set account globally.
   */
  val KEYCODE_PROFILE_SWITCH = 288

}
