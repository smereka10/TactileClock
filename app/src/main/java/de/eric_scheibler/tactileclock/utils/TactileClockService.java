package de.eric_scheibler.tactileclock.utils;

            import android.os.Handler;
            import android.os.Looper;
import java.util.Calendar;

import android.annotation.TargetApi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.media.AudioManager;

import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import de.eric_scheibler.tactileclock.R;
import de.eric_scheibler.tactileclock.data.HourFormat;
import de.eric_scheibler.tactileclock.data.TimeComponentOrder;
import de.eric_scheibler.tactileclock.ui.activity.MainActivity;
import timber.log.Timber;
import android.annotation.SuppressLint;
import android.content.pm.ServiceInfo;
import androidx.core.app.ServiceCompat;
import android.app.ForegroundServiceStartNotAllowedException;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class TactileClockService extends Service {

    // actions
    public static final String ACTION_UPDATE_NOTIFICATION = "de.eric_scheibler.tactileclock.action.update_notification";
    public static final String ACTION_VIBRATE_TIME = "de.eric_scheibler.tactileclock.action.vibrate_time";
    public static final String ACTION_VIBRATE_TEST_TIME = "de.eric_scheibler.tactileclock.action.vibrate_test_time";
    public static final String ACTION_VIBRATE_TIME_AND_SET_NEXT_ALARM = "de.eric_scheibler.tactileclock.action.vibrate_time_and_set_next_alarm";

    // broadcast responses
    public static final String VIBRATION_FINISHED = "de.eric_scheibler.tactileclock.response.vibration_finished";

    // testing
    public static int TEST_HOUR;
    public static int TEST_MINUTE;

    // service vars
    private long lastActivation;
    private ApplicationInstance applicationInstance;
    private AudioManager audioManager;
    private NotificationManager notificationManager;
    private ScreenReceiver mScreenReceiver;
    private SettingsManager settingsManagerInstance;

    @Override public void onCreate() {
        super.onCreate();
        lastActivation = 0l;
        Timber.d("onCreate");
        applicationInstance = (ApplicationInstance) ApplicationInstance.getContext();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        settingsManagerInstance = SettingsManager.getInstance();

        // register receiver that handles screen on and screen off logic
        // can't be done in manifest
        mScreenReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(mScreenReceiver, filter);

        if (settingsManagerInstance.isWatchEnabled()
                && ! ApplicationInstance.canScheduleExactAlarms()) {
            settingsManagerInstance.disableWatch();
        }
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public void onStart(Intent intent, int startId) {
        if (intent != null) {
            Timber.d("action: %1$s", intent.getAction());

            if (ACTION_UPDATE_NOTIFICATION.equals(intent.getAction())) {
                startForegroundService();
                if (shouldDestroyService()) {
                    destroyService();
                }

            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                vibrator.cancel(); // vibration will be canceled if screen is turned on while vibrating
                long activationTimeDifference = System.currentTimeMillis() - lastActivation;
                Timber.d("diff: %1$d = %2$d - %3$d", activationTimeDifference, System.currentTimeMillis(), lastActivation);
                if (settingsManagerInstance.getPowerButtonServiceEnabled()
                        && settingsManagerInstance.getPowerButtonErrorVibration()
                        && activationTimeDifference > settingsManagerInstance.getPowerButtonLowerErrorBoundary()
                        && activationTimeDifference < settingsManagerInstance.getPowerButtonUpperErrorBoundary()) {
                    // double click detected
                    // but screen was turned off and on instead of on and off
                    // vibrate error message
                    Helper.vibrateOnce(settingsManagerInstance.getLongVibration() * 2);
                }
                lastActivation = System.currentTimeMillis();

            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                long activationTimeDifference = System.currentTimeMillis() - lastActivation;
                Timber.d("diff: %1$d = %2$d - %3$d", activationTimeDifference, System.currentTimeMillis(), lastActivation);
                if (settingsManagerInstance.getPowerButtonServiceEnabled()
                        && activationTimeDifference > settingsManagerInstance.getPowerButtonLowerSuccessBoundary()
                        && activationTimeDifference < settingsManagerInstance.getPowerButtonUpperSuccessBoundary()) {
                    // double click detected
                    // screen was turned on and off correctly
                    // vibrate time
                    vibrateTime(false, false, false);
                }
                lastActivation = System.currentTimeMillis();

            } else if (ACTION_VIBRATE_TIME.equals(intent.getAction())) {
                vibrateTime(false, false, false);

            } else if (ACTION_VIBRATE_TEST_TIME.equals(intent.getAction())) {
                vibrateTime(false, false, true);

            } else if (ACTION_VIBRATE_TIME_AND_SET_NEXT_ALARM.equals(intent.getAction())) {
                // vibrate current time
                if (this.isVibrationAllowed()) {
                    vibrateTime(
                            settingsManagerInstance.getWatchAnnouncementVibration(),
                            settingsManagerInstance.getWatchOnlyVibrateMinutes(),
                            false);
                }

                // set next alarm
                applicationInstance.setAlarmAtFullMinute(
                        settingsManagerInstance.getWatchVibrationIntervalInMinutes());
            }
        }
    }

    private void startForegroundService() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST : 0;
        try {
            ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, getServiceNotification(), type);
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && e instanceof ForegroundServiceStartNotAllowedException) {
                // App not in a valid state to start foreground service
                Timber.e("ForegroundServiceStartNotAllowedException");
                destroyService();
            }
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy");
        try {
            if (mScreenReceiver != null) {
                unregisterReceiver(mScreenReceiver);
            }
        } catch (IllegalArgumentException e) {}
        destroyService();
    }

    private boolean shouldDestroyService() {
        return ! settingsManagerInstance.getPowerButtonServiceEnabled()
            && ! settingsManagerInstance.isWatchEnabled();
    }

    private void destroyService() {
        notificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        stopSelf();
    }


    /**
     * vibration pattern functions
     */

    private void vibrateTime(boolean announcementVibration, boolean minutesOnly, boolean testTime) {
        if (testTime) {
            vibrateTime(announcementVibration, minutesOnly, TEST_HOUR, TEST_MINUTE);
            return;
        }

        // get current time
        int hours, minutes;
        Calendar c = Calendar.getInstance();
        if (settingsManagerInstance.getHourFormat() == HourFormat.TWELVE_HOURS) {
            // 12 hour format
            hours = c.get(Calendar.HOUR);
            if (hours == 0)
                hours = 12;
        } else {
            // 24 hour format
            hours = c.get(Calendar.HOUR_OF_DAY);
        }
        minutes = c.get(Calendar.MINUTE);

        vibrateTime(announcementVibration, minutesOnly, hours, minutes);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void vibrateTime(boolean announcementVibration, boolean minutesOnly, int hours, int minutes) {
        long LONG_GAP = settingsManagerInstance.getLongGap();

        // Round minutes to the nearest multiple of 5 using integer arithmetic.
        // (minutes + 2) shifts the value so integer division truncates correctly.
        if (settingsManagerInstance.getWatchRoundMinutes())
            minutes = (minutes + 2) / 5 * 5;

        // Increase hour if minutes rounded to 60
        if (minutes == 60){
            minutes = 0;
            hours++;
        }

        // create vibration pattern
        long[] pattern = {LONG_GAP};

        // announcement vibration
        if (announcementVibration) {
            pattern = concat(
                    pattern,
                    new long[]{settingsManagerInstance.getLongVibration() * 2, SettingsManager.DEFAULT_LONG_GAP});
        }
        // hours and minutes
        if (settingsManagerInstance.getTimeComponentOrder() == TimeComponentOrder.MINUTES_HOURS) {
            // minutes first
            pattern = concat(pattern, getVibrationPatternForMinutes(minutes));
            // hours
            if (! minutesOnly) {
                // long gap between hours and minutes
                pattern = concat(pattern, new long[]{LONG_GAP});
                // then hours
                pattern = concat(pattern, getVibrationPatternForHours(hours));
            }
        } else {
            // hours
            if (! minutesOnly) {
                // hours first
                pattern = concat(pattern, getVibrationPatternForHours(hours));
                // long gap between hours and minutes
                pattern = concat(pattern, new long[]{LONG_GAP});
            }
            // then minutes
            pattern = concat(pattern, getVibrationPatternForMinutes(minutes));
        }

        // total duration
        long totalDuration = 0l;
        for (long duration : pattern) {
            totalDuration += duration;
        }

        // start vibration
        Helper.vibratePattern(pattern);

        // send vibration finished broadcast
        new Handler(Looper.getMainLooper()).postDelayed(
                new Runnable() {
                    @Override public void run() {
                        LocalBroadcastManager.getInstance(TactileClockService.this)
                            .sendBroadcast(new Intent(VIBRATION_FINISHED));
                    }
                }, totalDuration + 500l);
    }

    private long[] getVibrationPatternForHours(int hours) {
        long MEDIUM_GAP = settingsManagerInstance.getMediumGap();

        long[] pattern = new long[]{};
        // only add first digit of hour if it's bigger than 9 (Remove leading 0)
        if (hours > 9) {
            // first number of hour
            pattern = concat(pattern, getVibrationPatternForDigit(hours/10));
            // medium gap between first and second number of hours
            pattern = concat(pattern, new long[]{MEDIUM_GAP});
        }
        // second number of hour
        pattern = concat(pattern, getVibrationPatternForDigit(hours%10));
        return pattern;
    }

    private long[] getVibrationPatternForMinutes(int minutes) {
        long MEDIUM_GAP = settingsManagerInstance.getMediumGap();

        long[] pattern = new long[]{};
        if (minutes > 9) {
            // First number of minute
            pattern = concat(pattern, getVibrationPatternForDigit(minutes / 10));
            // medium gap between first and second number of minutes
            pattern = concat(pattern, new long[]{MEDIUM_GAP});
        }
        // second number of minute
        pattern = concat(pattern, getVibrationPatternForDigit(minutes%10));
        return pattern;
    }

    private long[] getVibrationPatternForDigit(int digit) {
        long DOT = settingsManagerInstance.getShortVibration();
        long DASH = settingsManagerInstance.getLongVibration();
        long GAP = settingsManagerInstance.getShortGap();

        switch (digit) {
            case 0: return new long[]{DASH, GAP, DASH};
            case 1: return new long[]{DOT};
            case 2: return new long[]{DOT, GAP, DOT};
            case 3: return new long[]{DOT, GAP, DOT, GAP, DOT};
            case 4: return new long[]{DOT, GAP, DOT, GAP, DOT, GAP, DOT};
            case 5: return new long[]{DASH};
            case 6: return new long[]{DASH, GAP, DOT};
            case 7: return new long[]{DASH, GAP, DOT, GAP, DOT};
            case 8: return new long[]{DASH, GAP, DOT, GAP, DOT, GAP, DOT};
            case 9: return new long[]{DASH, GAP, DOT, GAP, DOT, GAP, DOT, GAP, DOT};
            default: return new long[]{};
        }
    }

    private long[] concat(long[] array1, long[] array2) {
        int array1Len = array1.length;
        int array2Len = array2.length;
        long[] arrayResult = new long[array1Len+array2Len];
        System.arraycopy(array1, 0, arrayResult, 0, array1Len);
        System.arraycopy(array2, 0, arrayResult, array1Len, array2Len);
        return arrayResult;
    }


    /**
     * notification
     */
    private static final int NOTIFICATION_ID = 91223;

    private Notification getServiceNotification() {
        // launch MainActivity intent
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, getPendingIntentFlags());
        // notification message text
        String notificationMessage;
        if (settingsManagerInstance.isWatchEnabled()) {
            notificationMessage = String.format(
                    getResources().getString(R.string.serviceNotificationWatchEnabled),
                    enabledOrDisabled(settingsManagerInstance.getPowerButtonServiceEnabled()),
                    enabledOrDisabled(settingsManagerInstance.isWatchEnabled()),
                    getResources().getQuantityString(
                        R.plurals.minutes,
                        settingsManagerInstance.getWatchVibrationIntervalInMinutes(),
                        settingsManagerInstance.getWatchVibrationIntervalInMinutes()));
        } else {
            notificationMessage = String.format(
                    getResources().getString(R.string.serviceNotification),
                    enabledOrDisabled(settingsManagerInstance.getPowerButtonServiceEnabled()),
                    enabledOrDisabled(settingsManagerInstance.isWatchEnabled()));
        }
        // return notification
        return new NotificationCompat.Builder(this)
            .setChannelId(ApplicationInstance.NOTIFICATION_CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentText(notificationMessage)
            .build();
    }

    private String enabledOrDisabled(boolean enabled) {
        if (enabled) {
            return getResources().getString(R.string.dialogEnabled);
        }
        return getResources().getString(R.string.dialogDisabled);
    }

    @SuppressLint("Deprecation, UnspecifiedImmutableFlag")
    private static int getPendingIntentFlags() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
    }


    /**
     * do not desturb and active call
     */

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isVibrationAllowed() {
        // do not desturb
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (notificationManager.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) {
                return false;
            }
        }
        // active call
        if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
            return false;
        }
        // else allow
        return true;
    }

}
