package asura.ui.codec.android

// copied and modified from <https://android.googlesource.com/platform/frameworks/native/+/master/include/android/input.h>
// blob: dbfd61eb0565e1fa180f6b6c5d2d2263b02bef84
object AndroidInput {

  /**
   * Key states (may be returned by queries about the current state of a
   * particular key code, scan code or switch).
   */
  object AndroidKeyState {
    /** The key state is unknown or the requested key itself is not supported. */
    val AKEY_STATE_UNKNOWN = -1
    /** The key is up. */
    val AKEY_STATE_UP = 0
    /** The key is down. */
    val AKEY_STATE_DOWN = 1
    /** The key is down but is a virtual key press that is being emulated by the system. */
    val AKEY_STATE_VIRTUAL = 2
  }

  /**
   * Meta key / modifier state.
   */
  object AndroidMetaState {
    /** No meta keys are pressed. */
    val AMETA_NONE = 0
    /** This mask is used to check whether one of the ALT meta keys is pressed. */
    val AMETA_ALT_ON = 0x02
    /** This mask is used to check whether the left ALT meta key is pressed. */
    val AMETA_ALT_LEFT_ON = 0x10
    /** This mask is used to check whether the right ALT meta key is pressed. */
    val AMETA_ALT_RIGHT_ON = 0x20
    /** This mask is used to check whether one of the SHIFT meta keys is pressed. */
    val AMETA_SHIFT_ON = 0x01
    /** This mask is used to check whether the left SHIFT meta key is pressed. */
    val AMETA_SHIFT_LEFT_ON = 0x40
    /** This mask is used to check whether the right SHIFT meta key is pressed. */
    val AMETA_SHIFT_RIGHT_ON = 0x80
    /** This mask is used to check whether the SYM meta key is pressed. */
    val AMETA_SYM_ON = 0x04
    /** This mask is used to check whether the FUNCTION meta key is pressed. */
    val AMETA_FUNCTION_ON = 0x08
    /** This mask is used to check whether one of the CTRL meta keys is pressed. */
    val AMETA_CTRL_ON = 0x1000
    /** This mask is used to check whether the left CTRL meta key is pressed. */
    val AMETA_CTRL_LEFT_ON = 0x2000
    /** This mask is used to check whether the right CTRL meta key is pressed. */
    val AMETA_CTRL_RIGHT_ON = 0x4000
    /** This mask is used to check whether one of the META meta keys is pressed. */
    val AMETA_META_ON = 0x10000
    /** This mask is used to check whether the left META meta key is pressed. */
    val AMETA_META_LEFT_ON = 0x20000
    /** This mask is used to check whether the right META meta key is pressed. */
    val AMETA_META_RIGHT_ON = 0x40000
    /** This mask is used to check whether the CAPS LOCK meta key is on. */
    val AMETA_CAPS_LOCK_ON = 0x100000
    /** This mask is used to check whether the NUM LOCK meta key is on. */
    val AMETA_NUM_LOCK_ON = 0x200000
    /** This mask is used to check whether the SCROLL LOCK meta key is on. */
    val AMETA_SCROLL_LOCK_ON = 0x400000
  }

  /**
   * Input event types.
   */
  object AndroidInputEventType {
    /** Indicates that the input event is a key event. */
    val AINPUT_EVENT_TYPE_KEY = 1
    /** Indicates that the input event is a motion event. */
    val AINPUT_EVENT_TYPE_MOTION = 2
    /** Focus event */
    val AINPUT_EVENT_TYPE_FOCUS = 3
  }

  /**
   * Key event actions.
   */
  object AndroidKeyEventAction {
    /** The key has been pressed down. *//** The key has been pressed down. */
    val AKEY_EVENT_ACTION_DOWN = 0
    /** The key has been released. */
    val AKEY_EVENT_ACTION_UP = 1
    /**
     * Multiple duplicate key events have occurred in a row, or a
     * complex string is being delivered.  The repeat_count property
     * of the key event contains the number of times the given key
     * code should be executed.
     */
    val AKEY_EVENT_ACTION_MULTIPLE = 2
  }

  /**
   * Key event flags.
   */
  object AndroidKeyEventFlags {
    /** This mask is set if the device woke because of this key event. */
    val AKEY_EVENT_FLAG_WOKE_HERE = 0x1
    /** This mask is set if the key event was generated by a software keyboard. */
    val AKEY_EVENT_FLAG_SOFT_KEYBOARD = 0x2
    /** This mask is set if we don't want the key event to cause us to leave touch mode. */
    val AKEY_EVENT_FLAG_KEEP_TOUCH_MODE = 0x4
    /**
     * This mask is set if an event was known to come from a trusted
     * part of the system.  That is, the event is known to come from
     * the user, and could not have been spoofed by a third party
     * component.
     */
    val AKEY_EVENT_FLAG_FROM_SYSTEM = 0x8
    /**
     * This mask is used for compatibility, to identify enter keys that are
     * coming from an IME whose enter key has been auto-labelled "next" or
     * "done".  This allows TextView to dispatch these as normal enter keys
     * for old applications, but still do the appropriate action when
     * receiving them.
     */
    val AKEY_EVENT_FLAG_EDITOR_ACTION = 0x10
    /**
     * When associated with up key events, this indicates that the key press
     * has been canceled.  Typically this is used with virtual touch screen
     * keys, where the user can slide from the virtual key area on to the
     * display: in that case, the application will receive a canceled up
     * event and should not perform the action normally associated with the
     * key.  Note that for this to work, the application can not perform an
     * action for a key until it receives an up or the long press timeout has
     * expired.
     */
    val AKEY_EVENT_FLAG_CANCELED = 0x20
    /**
     * This key event was generated by a virtual (on-screen) hard key area.
     * Typically this is an area of the touchscreen, outside of the regular
     * display, dedicated to "hardware" buttons.
     */
    val AKEY_EVENT_FLAG_VIRTUAL_HARD_KEY = 0x40
    /**
     * This flag is set for the first key repeat that occurs after the
     * long press timeout.
     */
    val AKEY_EVENT_FLAG_LONG_PRESS = 0x80
    /**
     * Set when a key event has AKEY_EVENT_FLAG_CANCELED set because a long
     * press action was executed while it was down.
     */
    val AKEY_EVENT_FLAG_CANCELED_LONG_PRESS = 0x100
    /**
     * Set for AKEY_EVENT_ACTION_UP when this event's key code is still being
     * tracked from its initial down.  That is, somebody requested that tracking
     * started on the key down and a long press has not caused
     * the tracking to be canceled.
     */
    val AKEY_EVENT_FLAG_TRACKING = 0x200
    /**
     * Set when a key event has been synthesized to implement default behavior
     * for an event that the application did not handle.
     * Fallback key events are generated by unhandled trackball motions
     * (to emulate a directional keypad) and by certain unhandled key presses
     * that are declared in the key map (such as special function numeric keypad
     * keys when numlock is off).
     */
    val AKEY_EVENT_FLAG_FALLBACK = 0x400
  }

  /**
   * Bit shift for the action bits holding the pointer index as
   * defined by AMOTION_EVENT_ACTION_POINTER_INDEX_MASK.
   */
  val AMOTION_EVENT_ACTION_POINTER_INDEX_SHIFT = 8

  /** Motion event actions */
  object AndroidMotionEventAction {
    /** Bit mask of the parts of the action code that are the action itself. */
    val AMOTION_EVENT_ACTION_MASK = 0xff
    /**
     * Bits in the action code that represent a pointer index, used with
     * AMOTION_EVENT_ACTION_POINTER_DOWN and AMOTION_EVENT_ACTION_POINTER_UP.  Shifting
     * down by AMOTION_EVENT_ACTION_POINTER_INDEX_SHIFT provides the actual pointer
     * index where the data for the pointer going up or down can be found.
     */
    val AMOTION_EVENT_ACTION_POINTER_INDEX_MASK = 0xff00
    /** A pressed gesture has started, the motion contains the initial starting location. */
    val AMOTION_EVENT_ACTION_DOWN = 0
    /**
     * A pressed gesture has finished, the motion contains the final release location
     * as well as any intermediate points since the last down or move event.
     */
    val AMOTION_EVENT_ACTION_UP = 1
    /**
     * A change has happened during a press gesture (between AMOTION_EVENT_ACTION_DOWN and
     * AMOTION_EVENT_ACTION_UP).  The motion contains the most recent point, as well as
     * any intermediate points since the last down or move event.
     */
    val AMOTION_EVENT_ACTION_MOVE = 2
    /**
     * The current gesture has been aborted.
     * You will not receive any more points in it.  You should treat this as
     * an up event, but not perform any action that you normally would.
     */
    val AMOTION_EVENT_ACTION_CANCEL = 3
    /**
     * A movement has happened outside of the normal bounds of the UI element.
     * This does not provide a full gesture, but only the initial location of the movement/touch.
     */
    val AMOTION_EVENT_ACTION_OUTSIDE = 4
    /**
     * A non-primary pointer has gone down.
     * The bits in AMOTION_EVENT_ACTION_POINTER_INDEX_MASK indicate which pointer changed.
     */
    val AMOTION_EVENT_ACTION_POINTER_DOWN = 5
    /**
     * A non-primary pointer has gone up.
     * The bits in AMOTION_EVENT_ACTION_POINTER_INDEX_MASK indicate which pointer changed.
     */
    val AMOTION_EVENT_ACTION_POINTER_UP = 6
    /**
     * A change happened but the pointer is not down (unlike AMOTION_EVENT_ACTION_MOVE).
     * The motion contains the most recent point, as well as any intermediate points since
     * the last hover move event.
     */
    val AMOTION_EVENT_ACTION_HOVER_MOVE = 7
    /**
     * The motion event contains relative vertical and/or horizontal scroll offsets.
     * Use getAxisValue to retrieve the information from AMOTION_EVENT_AXIS_VSCROLL
     * and AMOTION_EVENT_AXIS_HSCROLL.
     * The pointer may or may not be down when this event is dispatched.
     * This action is always delivered to the winder under the pointer, which
     * may not be the window currently touched.
     */
    val AMOTION_EVENT_ACTION_SCROLL = 8
    /** The pointer is not down but has entered the boundaries of a window or view. */
    val AMOTION_EVENT_ACTION_HOVER_ENTER = 9
    /** The pointer is not down but has exited the boundaries of a window or view. */
    val AMOTION_EVENT_ACTION_HOVER_EXIT = 10
    /* One or more buttons have been pressed. */
    val AMOTION_EVENT_ACTION_BUTTON_PRESS = 11
    /* One or more buttons have been released. */
    val AMOTION_EVENT_ACTION_BUTTON_RELEASE = 12
  }

  /**
   * Motion event flags.
   */
  object AndroidMotionEventFlags {
    /**
     * This flag indicates that the window that received this motion event is partly
     * or wholly obscured by another visible window above it.  This flag is set to true
     * even if the event did not directly pass through the obscured area.
     * A security sensitive application can check this flag to identify situations in which
     * a malicious application may have covered up part of its content for the purpose
     * of misleading the user or hijacking touches.  An appropriate response might be
     * to drop the suspect touches or to take additional precautions to confirm the user's
     * actual intent.
     */
    val AMOTION_EVENT_FLAG_WINDOW_IS_OBSCURED = 0x1
  }

  /**
   * Motion event edge touch flags.
   */
  object AndroidMotionEventEdgeTouchFlags {
    /** No edges intersected. *//** No edges intersected. */
    val AMOTION_EVENT_EDGE_FLAG_NONE = 0
    /** Flag indicating the motion event intersected the top edge of the screen. */
    val AMOTION_EVENT_EDGE_FLAG_TOP = 0x01
    /** Flag indicating the motion event intersected the bottom edge of the screen. */
    val AMOTION_EVENT_EDGE_FLAG_BOTTOM = 0x02
    /** Flag indicating the motion event intersected the left edge of the screen. */
    val AMOTION_EVENT_EDGE_FLAG_LEFT = 0x04
    /** Flag indicating the motion event intersected the right edge of the screen. */
    val AMOTION_EVENT_EDGE_FLAG_RIGHT = 0x08
  }

  /**
   * Constants that identify each individual axis of a motion event.
   *
   * @anchor AMOTION_EVENT_AXIS
   */
  object AndroidMotionEventAxis {
    /**
     * Axis constant: X axis of a motion event.
     *
     * - For a touch screen, reports the absolute X screen position of the center of
     * the touch contact area.  The units are display pixels.
     * - For a touch pad, reports the absolute X surface position of the center of the touch
     * contact area. The units are device-dependent.
     * - For a mouse, reports the absolute X screen position of the mouse pointer.
     * The units are display pixels.
     * - For a trackball, reports the relative horizontal displacement of the trackball.
     * The value is normalized to a range from -1.0 (left) to 1.0 (right).
     * - For a joystick, reports the absolute X position of the joystick.
     * The value is normalized to a range from -1.0 (left) to 1.0 (right).
     */
    val AMOTION_EVENT_AXIS_X = 0
    /**
     * Axis constant: Y axis of a motion event.
     *
     * - For a touch screen, reports the absolute Y screen position of the center of
     * the touch contact area.  The units are display pixels.
     * - For a touch pad, reports the absolute Y surface position of the center of the touch
     * contact area. The units are device-dependent.
     * - For a mouse, reports the absolute Y screen position of the mouse pointer.
     * The units are display pixels.
     * - For a trackball, reports the relative vertical displacement of the trackball.
     * The value is normalized to a range from -1.0 (up) to 1.0 (down).
     * - For a joystick, reports the absolute Y position of the joystick.
     * The value is normalized to a range from -1.0 (up or far) to 1.0 (down or near).
     */
    val AMOTION_EVENT_AXIS_Y = 1
    /**
     * Axis constant: Pressure axis of a motion event.
     *
     * - For a touch screen or touch pad, reports the approximate pressure applied to the surface
     * by a finger or other tool.  The value is normalized to a range from
     * 0 (no pressure at all) to 1 (normal pressure), although values higher than 1
     * may be generated depending on the calibration of the input device.
     * - For a trackball, the value is set to 1 if the trackball button is pressed
     * or 0 otherwise.
     * - For a mouse, the value is set to 1 if the primary mouse button is pressed
     * or 0 otherwise.
     */
    val AMOTION_EVENT_AXIS_PRESSURE = 2
    /**
     * Axis constant: Size axis of a motion event.
     *
     * - For a touch screen or touch pad, reports the approximate size of the contact area in
     * relation to the maximum detectable size for the device.  The value is normalized
     * to a range from 0 (smallest detectable size) to 1 (largest detectable size),
     * although it is not a linear scale. This value is of limited use.
     * To obtain calibrated size information, see
     * {@link AMOTION_EVENT_AXIS_TOUCH_MAJOR} or {@link AMOTION_EVENT_AXIS_TOOL_MAJOR}.
     */
    val AMOTION_EVENT_AXIS_SIZE = 3
    /**
     * Axis constant: TouchMajor axis of a motion event.
     *
     * - For a touch screen, reports the length of the major axis of an ellipse that
     * represents the touch area at the point of contact.
     * The units are display pixels.
     * - For a touch pad, reports the length of the major axis of an ellipse that
     * represents the touch area at the point of contact.
     * The units are device-dependent.
     */
    val AMOTION_EVENT_AXIS_TOUCH_MAJOR = 4
    /**
     * Axis constant: TouchMinor axis of a motion event.
     *
     * - For a touch screen, reports the length of the minor axis of an ellipse that
     * represents the touch area at the point of contact.
     * The units are display pixels.
     * - For a touch pad, reports the length of the minor axis of an ellipse that
     * represents the touch area at the point of contact.
     * The units are device-dependent.
     *
     * When the touch is circular, the major and minor axis lengths will be equal to one another.
     */
    val AMOTION_EVENT_AXIS_TOUCH_MINOR = 5
    /**
     * Axis constant: ToolMajor axis of a motion event.
     *
     * - For a touch screen, reports the length of the major axis of an ellipse that
     * represents the size of the approaching finger or tool used to make contact.
     * - For a touch pad, reports the length of the major axis of an ellipse that
     * represents the size of the approaching finger or tool used to make contact.
     * The units are device-dependent.
     *
     * When the touch is circular, the major and minor axis lengths will be equal to one another.
     *
     * The tool size may be larger than the touch size since the tool may not be fully
     * in contact with the touch sensor.
     */
    val AMOTION_EVENT_AXIS_TOOL_MAJOR = 6
    /**
     * Axis constant: ToolMinor axis of a motion event.
     *
     * - For a touch screen, reports the length of the minor axis of an ellipse that
     * represents the size of the approaching finger or tool used to make contact.
     * - For a touch pad, reports the length of the minor axis of an ellipse that
     * represents the size of the approaching finger or tool used to make contact.
     * The units are device-dependent.
     *
     * When the touch is circular, the major and minor axis lengths will be equal to one another.
     *
     * The tool size may be larger than the touch size since the tool may not be fully
     * in contact with the touch sensor.
     */
    val AMOTION_EVENT_AXIS_TOOL_MINOR = 7
    /**
     * Axis constant: Orientation axis of a motion event.
     *
     * - For a touch screen or touch pad, reports the orientation of the finger
     * or tool in radians relative to the vertical plane of the device.
     * An angle of 0 radians indicates that the major axis of contact is oriented
     * upwards, is perfectly circular or is of unknown orientation.  A positive angle
     * indicates that the major axis of contact is oriented to the right.  A negative angle
     * indicates that the major axis of contact is oriented to the left.
     * The full range is from -PI/2 radians (finger pointing fully left) to PI/2 radians
     * (finger pointing fully right).
     * - For a stylus, the orientation indicates the direction in which the stylus
     * is pointing in relation to the vertical axis of the current orientation of the screen.
     * The range is from -PI radians to PI radians, where 0 is pointing up,
     * -PI/2 radians is pointing left, -PI or PI radians is pointing down, and PI/2 radians
     * is pointing right.  See also {@link AMOTION_EVENT_AXIS_TILT}.
     */
    val AMOTION_EVENT_AXIS_ORIENTATION = 8
    /**
     * Axis constant: Vertical Scroll axis of a motion event.
     *
     * - For a mouse, reports the relative movement of the vertical scroll wheel.
     * The value is normalized to a range from -1.0 (down) to 1.0 (up).
     *
     * This axis should be used to scroll views vertically.
     */
    val AMOTION_EVENT_AXIS_VSCROLL = 9
    /**
     * Axis constant: Horizontal Scroll axis of a motion event.
     *
     * - For a mouse, reports the relative movement of the horizontal scroll wheel.
     * The value is normalized to a range from -1.0 (left) to 1.0 (right).
     *
     * This axis should be used to scroll views horizontally.
     */
    val AMOTION_EVENT_AXIS_HSCROLL = 10
    /**
     * Axis constant: Z axis of a motion event.
     *
     * - For a joystick, reports the absolute Z position of the joystick.
     * The value is normalized to a range from -1.0 (high) to 1.0 (low).
     * <em>On game pads with two analog joysticks, this axis is often reinterpreted
     * to report the absolute X position of the second joystick instead.</em>
     */
    val AMOTION_EVENT_AXIS_Z = 11
    /**
     * Axis constant: X Rotation axis of a motion event.
     *
     * - For a joystick, reports the absolute rotation angle about the X axis.
     * The value is normalized to a range from -1.0 (counter-clockwise) to 1.0 (clockwise).
     */
    val AMOTION_EVENT_AXIS_RX = 12
    /**
     * Axis constant: Y Rotation axis of a motion event.
     *
     * - For a joystick, reports the absolute rotation angle about the Y axis.
     * The value is normalized to a range from -1.0 (counter-clockwise) to 1.0 (clockwise).
     */
    val AMOTION_EVENT_AXIS_RY = 13
    /**
     * Axis constant: Z Rotation axis of a motion event.
     *
     * - For a joystick, reports the absolute rotation angle about the Z axis.
     * The value is normalized to a range from -1.0 (counter-clockwise) to 1.0 (clockwise).
     * On game pads with two analog joysticks, this axis is often reinterpreted
     * to report the absolute Y position of the second joystick instead.
     */
    val AMOTION_EVENT_AXIS_RZ = 14
    /**
     * Axis constant: Hat X axis of a motion event.
     *
     * - For a joystick, reports the absolute X position of the directional hat control.
     * The value is normalized to a range from -1.0 (left) to 1.0 (right).
     */
    val AMOTION_EVENT_AXIS_HAT_X = 15
    /**
     * Axis constant: Hat Y axis of a motion event.
     *
     * - For a joystick, reports the absolute Y position of the directional hat control.
     * The value is normalized to a range from -1.0 (up) to 1.0 (down).
     */
    val AMOTION_EVENT_AXIS_HAT_Y = 16
    /**
     * Axis constant: Left Trigger axis of a motion event.
     *
     * - For a joystick, reports the absolute position of the left trigger control.
     * The value is normalized to a range from 0.0 (released) to 1.0 (fully pressed).
     */
    val AMOTION_EVENT_AXIS_LTRIGGER = 17
    /**
     * Axis constant: Right Trigger axis of a motion event.
     *
     * - For a joystick, reports the absolute position of the right trigger control.
     * The value is normalized to a range from 0.0 (released) to 1.0 (fully pressed).
     */
    val AMOTION_EVENT_AXIS_RTRIGGER = 18
    /**
     * Axis constant: Throttle axis of a motion event.
     *
     * - For a joystick, reports the absolute position of the throttle control.
     * The value is normalized to a range from 0.0 (fully open) to 1.0 (fully closed).
     */
    val AMOTION_EVENT_AXIS_THROTTLE = 19
    /**
     * Axis constant: Rudder axis of a motion event.
     *
     * - For a joystick, reports the absolute position of the rudder control.
     * The value is normalized to a range from -1.0 (turn left) to 1.0 (turn right).
     */
    val AMOTION_EVENT_AXIS_RUDDER = 20
    /**
     * Axis constant: Wheel axis of a motion event.
     *
     * - For a joystick, reports the absolute position of the steering wheel control.
     * The value is normalized to a range from -1.0 (turn left) to 1.0 (turn right).
     */
    val AMOTION_EVENT_AXIS_WHEEL = 21
    /**
     * Axis constant: Gas axis of a motion event.
     *
     * - For a joystick, reports the absolute position of the gas (accelerator) control.
     * The value is normalized to a range from 0.0 (no acceleration)
     * to 1.0 (maximum acceleration).
     */
    val AMOTION_EVENT_AXIS_GAS = 22
    /**
     * Axis constant: Brake axis of a motion event.
     *
     * - For a joystick, reports the absolute position of the brake control.
     * The value is normalized to a range from 0.0 (no braking) to 1.0 (maximum braking).
     */
    val AMOTION_EVENT_AXIS_BRAKE = 23
    /**
     * Axis constant: Distance axis of a motion event.
     *
     * - For a stylus, reports the distance of the stylus from the screen.
     * A value of 0.0 indicates direct contact and larger values indicate increasing
     * distance from the surface.
     */
    val AMOTION_EVENT_AXIS_DISTANCE = 24
    /**
     * Axis constant: Tilt axis of a motion event.
     *
     * - For a stylus, reports the tilt angle of the stylus in radians where
     * 0 radians indicates that the stylus is being held perpendicular to the
     * surface, and PI/2 radians indicates that the stylus is being held flat
     * against the surface.
     */
    val AMOTION_EVENT_AXIS_TILT = 25
    /**
     * Axis constant:  Generic scroll axis of a motion event.
     *
     * - This is used for scroll axis motion events that can't be classified as strictly
     * vertical or horizontal. The movement of a rotating scroller is an example of this.
     */
    val AMOTION_EVENT_AXIS_SCROLL = 26
    /**
     * Axis constant: The movement of x position of a motion event.
     *
     * - For a mouse, reports a difference of x position between the previous position.
     * This is useful when pointer is captured, in that case the mouse pointer doesn't
     * change the location but this axis reports the difference which allows the app
     * to see how the mouse is moved.
     */
    val AMOTION_EVENT_AXIS_RELATIVE_X = 27
    /**
     * Axis constant: The movement of y position of a motion event.
     *
     * Same as {@link RELATIVE_X}, but for y position.
     */
    val AMOTION_EVENT_AXIS_RELATIVE_Y = 28
    /**
     * Axis constant: Generic 1 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_1 = 32
    /**
     * Axis constant: Generic 2 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_2 = 33
    /**
     * Axis constant: Generic 3 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_3 = 34
    /**
     * Axis constant: Generic 4 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_4 = 35
    /**
     * Axis constant: Generic 5 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_5 = 36
    /**
     * Axis constant: Generic 6 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_6 = 37
    /**
     * Axis constant: Generic 7 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_7 = 38
    /**
     * Axis constant: Generic 8 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_8 = 39
    /**
     * Axis constant: Generic 9 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_9 = 40
    /**
     * Axis constant: Generic 10 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_10 = 41
    /**
     * Axis constant: Generic 11 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_11 = 42
    /**
     * Axis constant: Generic 12 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_12 = 43
    /**
     * Axis constant: Generic 13 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_13 = 44
    /**
     * Axis constant: Generic 14 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_14 = 45
    /**
     * Axis constant: Generic 15 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_15 = 46
    /**
     * Axis constant: Generic 16 axis of a motion event.
     * The interpretation of a generic axis is device-specific.
     */
    val AMOTION_EVENT_AXIS_GENERIC_16 = 47

    // NOTE: If you add a new axis here you must also add it to several other files.
    //       Refer to frameworks/base/core/java/android/view/MotionEvent.java for the full list.
  }

  /**
   * Constants that identify buttons that are associated with motion events.
   * Refer to the documentation on the MotionEvent class for descriptions of each button.
   */
  object AndroidMotionEventButtons {
    /** primary */
    val AMOTION_EVENT_BUTTON_PRIMARY = 1 << 0
    /** secondary */
    val AMOTION_EVENT_BUTTON_SECONDARY = 1 << 1
    /** tertiary */
    val AMOTION_EVENT_BUTTON_TERTIARY = 1 << 2
    /** back */
    val AMOTION_EVENT_BUTTON_BACK = 1 << 3
    /** forward */
    val AMOTION_EVENT_BUTTON_FORWARD = 1 << 4
    val AMOTION_EVENT_BUTTON_STYLUS_PRIMARY = 1 << 5
    val AMOTION_EVENT_BUTTON_STYLUS_SECONDARY = 1 << 6
  }

  /**
   * Constants that identify tool types.
   * Refer to the documentation on the MotionEvent class for descriptions of each tool type.
   */
  object AndroidMotionEventToolType {
    /** unknown */
    val AMOTION_EVENT_TOOL_TYPE_UNKNOWN = 0
    /** finger */
    val AMOTION_EVENT_TOOL_TYPE_FINGER = 1
    /** stylus */
    val AMOTION_EVENT_TOOL_TYPE_STYLUS = 2
    /** mouse */
    val AMOTION_EVENT_TOOL_TYPE_MOUSE = 3
    /** eraser */
    val AMOTION_EVENT_TOOL_TYPE_ERASER = 4
    /** palm */
    val AMOTION_EVENT_TOOL_TYPE_PALM = 5
  }

  /**
   * Input source masks.
   *
   * Refer to the documentation on android.view.InputDevice for more details about input sources
   * and their correct interpretation.
   */
  object AndroidInputSourceClass {
    /** mask */
    val AINPUT_SOURCE_CLASS_MASK = 0x000000ff
    /** none */
    val AINPUT_SOURCE_CLASS_NONE = 0x00000000
    /** button */
    val AINPUT_SOURCE_CLASS_BUTTON = 0x00000001
    /** pointer */
    val AINPUT_SOURCE_CLASS_POINTER = 0x00000002
    /** navigation */
    val AINPUT_SOURCE_CLASS_NAVIGATION = 0x00000004
    /** position */
    val AINPUT_SOURCE_CLASS_POSITION = 0x00000008
    /** joystick */
    val AINPUT_SOURCE_CLASS_JOYSTICK = 0x00000010
  }

  /**
   * Input sources.
   */
  object AndroidInputSource {

    import AndroidInputSourceClass._

    /** unknown */
    val AINPUT_SOURCE_UNKNOWN = 0x00000000
    /** keyboard */
    val AINPUT_SOURCE_KEYBOARD = 0x00000100 | AINPUT_SOURCE_CLASS_BUTTON
    /** dpad */
    val AINPUT_SOURCE_DPAD = 0x00000200 | AINPUT_SOURCE_CLASS_BUTTON
    /** gamepad */
    val AINPUT_SOURCE_GAMEPAD = 0x00000400 | AINPUT_SOURCE_CLASS_BUTTON
    /** touchscreen */
    val AINPUT_SOURCE_TOUCHSCREEN = 0x00001000 | AINPUT_SOURCE_CLASS_POINTER
    /** mouse */
    val AINPUT_SOURCE_MOUSE = 0x00002000 | AINPUT_SOURCE_CLASS_POINTER
    /** stylus */
    val AINPUT_SOURCE_STYLUS = 0x00004000 | AINPUT_SOURCE_CLASS_POINTER
    /** bluetooth stylus */
    val AINPUT_SOURCE_BLUETOOTH_STYLUS = 0x00008000 | AINPUT_SOURCE_STYLUS
    /** trackball */
    val AINPUT_SOURCE_TRACKBALL = 0x00010000 | AINPUT_SOURCE_CLASS_NAVIGATION
    /** mouse relative */
    val AINPUT_SOURCE_MOUSE_RELATIVE = 0x00020000 | AINPUT_SOURCE_CLASS_NAVIGATION
    /** touchpad */
    val AINPUT_SOURCE_TOUCHPAD = 0x00100000 | AINPUT_SOURCE_CLASS_POSITION
    /** navigation */
    val AINPUT_SOURCE_TOUCH_NAVIGATION = 0x00200000 | AINPUT_SOURCE_CLASS_NONE
    /** joystick */
    val AINPUT_SOURCE_JOYSTICK = 0x01000000 | AINPUT_SOURCE_CLASS_JOYSTICK
    /** rotary encoder */
    val AINPUT_SOURCE_ROTARY_ENCODER = 0x00400000 | AINPUT_SOURCE_CLASS_NONE
    /** any */
    val AINPUT_SOURCE_ANY = 0xffffff00
  }

  /**
   * Keyboard types.
   *
   * Refer to the documentation on android.view.InputDevice for more details.
   */
  object AndroidKeyboardType {
    /** none */
    val AINPUT_KEYBOARD_TYPE_NONE = 0
    /** non alphabetic */
    val AINPUT_KEYBOARD_TYPE_NON_ALPHABETIC = 1
    /** alphabetic */
    val AINPUT_KEYBOARD_TYPE_ALPHABETIC = 2
  }

  /**
   * Constants used to retrieve information about the range of motion for a particular
   * coordinate of a motion event.
   *
   * Refer to the documentation on android.view.InputDevice for more details about input sources
   * and their correct interpretation.
   *
   * @deprecated These constants are deprecated. Use {@link AMOTION_EVENT_AXIS AMOTION_EVENT_AXIS_*} constants instead.
   */
  object AndroidMotionRange {

    import AndroidMotionEventAxis._

    /** x */
    val AINPUT_MOTION_RANGE_X = AMOTION_EVENT_AXIS_X
    /** y */
    val AINPUT_MOTION_RANGE_Y = AMOTION_EVENT_AXIS_Y
    /** pressure */
    val AINPUT_MOTION_RANGE_PRESSURE = AMOTION_EVENT_AXIS_PRESSURE
    /** size */
    val AINPUT_MOTION_RANGE_SIZE = AMOTION_EVENT_AXIS_SIZE
    /** touch major */
    val AINPUT_MOTION_RANGE_TOUCH_MAJOR = AMOTION_EVENT_AXIS_TOUCH_MAJOR
    /** touch minor */
    val AINPUT_MOTION_RANGE_TOUCH_MINOR = AMOTION_EVENT_AXIS_TOUCH_MINOR
    /** tool major */
    val AINPUT_MOTION_RANGE_TOOL_MAJOR = AMOTION_EVENT_AXIS_TOOL_MAJOR
    /** tool minor */
    val AINPUT_MOTION_RANGE_TOOL_MINOR = AMOTION_EVENT_AXIS_TOOL_MINOR
    /** orientation */
    val AINPUT_MOTION_RANGE_ORIENTATION = AMOTION_EVENT_AXIS_ORIENTATION
  }

}
