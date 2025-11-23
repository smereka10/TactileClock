package de.eric_scheibler.tactileclock.ui.fragment;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import de.eric_scheibler.tactileclock.R;
import de.eric_scheibler.tactileclock.ui.dialog.SelectIntegerDialog.IntegerSelector;
import de.eric_scheibler.tactileclock.ui.dialog.SelectIntegerDialog.Token;
import de.eric_scheibler.tactileclock.ui.dialog.SelectIntegerDialog;
import de.eric_scheibler.tactileclock.utils.SettingsManager;
import androidx.fragment.app.Fragment;
import android.annotation.TargetApi;
import android.os.Build;
import android.content.Intent;
import android.provider.Settings;
import de.eric_scheibler.tactileclock.utils.ApplicationInstance;


public class WatchFragment extends Fragment implements IntegerSelector {

	// Store instance variables
	private SettingsManager settingsManagerInstance;

    private androidx.appcompat.widget.SwitchCompat buttonStartWatch, buttonEnableVibration, buttonPlayGTS;
    private LinearLayout containerVibrationOptions;
    private Button buttonWatchInterval;
    private androidx.appcompat.widget.SwitchCompat buttonWatchOnlyVibrateMinutes, buttonWatchStartAtNextFullHour, buttonWatchAnnouncementVibration;

    // newInstance constructor for creating fragment with arguments
    public static WatchFragment newInstance() {
        WatchFragment watchFragmentInstance = new WatchFragment();
        return watchFragmentInstance;
    }

	@Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManagerInstance = SettingsManager.getInstance();
	}

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_watch, container, false);
    }

	@Override public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        buttonStartWatch = (androidx.appcompat.widget.SwitchCompat) view.findViewById(R.id.buttonStartWatch);
        buttonStartWatch.setOnCheckedChangeListener(null);

        buttonEnableVibration = (androidx.appcompat.widget.SwitchCompat) view.findViewById(R.id.switchEnableVibration);
        buttonEnableVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (settingsManagerInstance.getWatchEnableVibration() != isChecked) {
                    settingsManagerInstance.setWatchEnableVibration(isChecked);
                    updateUI();
                }
            }
        });

        containerVibrationOptions = (LinearLayout) view.findViewById(R.id.containerVibrationOptions);

        buttonWatchInterval = (Button) view.findViewById(R.id.buttonWatchInterval);
        buttonWatchInterval.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SelectIntegerDialog dialog = SelectIntegerDialog.newInstance(
                        Token.WATCH_INTERVAL,
                        settingsManagerInstance.getWatchVibrationIntervalInMinutes(),
                        SettingsManager.DEFAULT_WATCH_VIBRATION_INTERVAL);
                dialog.setTargetFragment(WatchFragment.this, 1);
                dialog.show(getActivity().getSupportFragmentManager(), "SelectIntegerDialog");
            }
        });

        buttonWatchOnlyVibrateMinutes = (androidx.appcompat.widget.SwitchCompat) view.findViewById(R.id.buttonWatchOnlyVibrateMinutes);
        buttonWatchOnlyVibrateMinutes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (settingsManagerInstance.getWatchOnlyVibrateMinutes() != isChecked) {
                    settingsManagerInstance.setWatchOnlyVibrateMinutes(isChecked);
                }
            }
        });

        buttonWatchStartAtNextFullHour = (androidx.appcompat.widget.SwitchCompat) view.findViewById(R.id.buttonWatchStartAtNextFullHour);
        buttonWatchStartAtNextFullHour.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (settingsManagerInstance.getWatchStartAtNextFullHour() != isChecked) {
                    settingsManagerInstance.setWatchStartAtNextFullHour(isChecked);
                }
            }
        });

        buttonWatchAnnouncementVibration = (androidx.appcompat.widget.SwitchCompat) view.findViewById(R.id.buttonWatchAnnouncementVibration);
        buttonWatchAnnouncementVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (settingsManagerInstance.getWatchAnnouncementVibration() != isChecked) {
                    settingsManagerInstance.setWatchAnnouncementVibration(isChecked);
                }
            }
        });

        buttonPlayGTS = (androidx.appcompat.widget.SwitchCompat) view.findViewById(R.id.buttonPlayGTS);
        buttonPlayGTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (settingsManagerInstance.getPlayGTS() != isChecked) {
                    settingsManagerInstance.setPlayGTS(isChecked);
                }
            }
        });
    }

    @Override public void onPause() {
        super.onPause();
    }

    @Override public void onResume() {
        super.onResume();
        if (settingsManagerInstance.isWatchEnabled()
                && ! ApplicationInstance.canScheduleExactAlarms()) {
            settingsManagerInstance.disableWatch();
        }

        updateUI();
    }

    @Override public void integerSelected(Token token, Integer newInteger) {
        if (newInteger != null) {
            switch (token) {
                case WATCH_INTERVAL:
                    settingsManagerInstance.setWatchVibrationIntervalInMinutes(newInteger);
                    updateUI();
                    break;
                default:
                    break;
            }
        }
    }

    private void updateUI() {
        buttonStartWatch.setChecked(
                settingsManagerInstance.isWatchEnabled());
        buttonStartWatch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (settingsManagerInstance.isWatchEnabled() != isChecked) {
                    if (! settingsManagerInstance.isWatchEnabled()) {
                        tryToEnableWatch();
                    } else {
                        settingsManagerInstance.disableWatch();
                    }
                    updateUI();
                }
            }
        });

        // Semi transparent elements when watch is disabled
        float alpha = settingsManagerInstance.isWatchEnabled() ? 0.4f : 1f;

        // Vibration
        buttonEnableVibration.setChecked(settingsManagerInstance.getWatchEnableVibration());
        buttonEnableVibration.setClickable(! settingsManagerInstance.isWatchEnabled());
        buttonEnableVibration.setAlpha(alpha);

        containerVibrationOptions.setVisibility(buttonEnableVibration.isChecked() ? View.VISIBLE : View.GONE);

        buttonWatchInterval.setText(
                String.format(
                    "%1$s: %2$s",
                    getResources().getString(R.string.buttonWatchInterval),
                    getResources().getQuantityString(
                        R.plurals.minutes,
                        settingsManagerInstance.getWatchVibrationIntervalInMinutes(),
                        settingsManagerInstance.getWatchVibrationIntervalInMinutes()))
                );
        buttonWatchInterval.setClickable(! settingsManagerInstance.isWatchEnabled());
        buttonWatchInterval.setAlpha(alpha);

        buttonWatchOnlyVibrateMinutes.setChecked(settingsManagerInstance.getWatchOnlyVibrateMinutes());
        buttonWatchOnlyVibrateMinutes.setClickable(! settingsManagerInstance.isWatchEnabled());
        buttonWatchOnlyVibrateMinutes.setAlpha(alpha);

        buttonWatchStartAtNextFullHour.setChecked(settingsManagerInstance.getWatchStartAtNextFullHour());
        buttonWatchStartAtNextFullHour.setClickable(! settingsManagerInstance.isWatchEnabled());
        buttonWatchStartAtNextFullHour.setAlpha(alpha);

        buttonWatchAnnouncementVibration.setChecked(settingsManagerInstance.getWatchAnnouncementVibration());
        buttonWatchAnnouncementVibration.setClickable(! settingsManagerInstance.isWatchEnabled());
        buttonWatchAnnouncementVibration.setAlpha(alpha);

        // GTS
        buttonPlayGTS.setChecked(settingsManagerInstance.getPlayGTS());
        buttonPlayGTS.setClickable(! settingsManagerInstance.isWatchEnabled());
        buttonPlayGTS.setAlpha(alpha);
    }

    @TargetApi(Build.VERSION_CODES.S)
    private void tryToEnableWatch() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (! ApplicationInstance.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }
        }
        settingsManagerInstance.enableWatch();
    }

}
