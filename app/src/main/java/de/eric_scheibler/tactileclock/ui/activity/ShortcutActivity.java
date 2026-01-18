package de.eric_scheibler.tactileclock.ui.activity;

import android.os.Handler;
import android.os.Looper;
import android.content.Intent;




import androidx.appcompat.app.AppCompatActivity;






import de.eric_scheibler.tactileclock.utils.TactileClockService;
import androidx.core.content.ContextCompat;
import de.eric_scheibler.tactileclock.utils.ApplicationInstance;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.IntentFilter;
import android.content.Context;


public class ShortcutActivity extends AppCompatActivity {

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(TactileClockService.VIBRATION_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);

        if (getIntent() != null
                && TactileClockService.ACTION_VIBRATE_TIME.equals(getIntent().getAction())) {

            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                Intent vibrateTimeIntent = new Intent(
                        ApplicationInstance.getContext(), TactileClockService.class);
                vibrateTimeIntent.setAction(TactileClockService.ACTION_VIBRATE_TIME);
                ContextCompat.startForegroundService(
                        ApplicationInstance.getContext(), vibrateTimeIntent);
            }, 250);    // extra delay for activity initialization

        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TactileClockService.VIBRATION_FINISHED)) {
                finishAndRemoveTask();
            }
        }
    };

}
