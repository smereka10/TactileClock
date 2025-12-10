Tactile Clock
=============

This Android app vibrates the current time when the display is locked and the power button is
clicked. If the double click is performed accidentally while the display is still active, the app 
can warn with a long, persistent vibration.

You may also use Tactile clock to keep informed about the current time. The app
can vibrate the current time automatically every X minutes or every hour.

It can also play the [Greenwich Time Signal](https://en.wikipedia.org/wiki/Greenwich_Time_Signal) at the start of each hour
similar to how a radio station would do.

The background process automatically initiates once the system has finished booting.


Two different vibration pattern exist:

A short vibration stands for the digit 1 and a long one for the digit 5. The 2
is represented by two consecutive short vibrations, the 6 by a long and a short one and so
on. The 0 constitutes an exception with two long vibrations.

**Examples:**

```
01:18 =  .    .  -...
02:51 =  ..    -  .
10:11 =  .  --    .  .
19:06 =  .  -....    -.
```

**Explanation:**

The time is processed digit by digit:

* . = short
* \- = long

Leading zeros in the hour and minute fields are omitted.

To simplify the recognition of the vibration pattern, there exist three kind of gaps with different durations:

* \[\]: A short gap between vibrations in the same digit
* \[&nbsp;&nbsp;\]: A medium gap for the separation of two digits within the hour and minute field
* \[&nbsp;&nbsp;&nbsp;&nbsp;\]: A long gap to split hours and minutes


The app supports all devices with Android version >= 5.1 (API 21).

This app is also available in the [Play Store](https://play.google.com/store/apps/details?id=de.eric_scheibler.tactileclock&hl=en)
and at [IzzyOnDroid](https://apt.izzysoft.de/packages/de.eric_scheibler.tactileclock).

