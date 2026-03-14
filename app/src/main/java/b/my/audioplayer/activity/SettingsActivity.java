package b.my.audioplayer.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import b.my.audioplayer.R;
import b.my.audioplayer.viewmodel.MainViewModel;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private MainViewModel viewModel;

    private ImageButton btnBack;
//    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchCrossfade;
    private LinearLayout layoutCrossfadeDuration;
    private SeekBar seekBarCrossfade;
    private TextView textCrossfadeDuration;
    private SwitchMaterial switchVolumeNormalization;
    private LinearLayout btnScanMedia;
    private TextView textAppVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackSettings);
//        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchCrossfade = findViewById(R.id.switchCrossfade);
        layoutCrossfadeDuration = findViewById(R.id.layoutCrossfadeDuration);
        seekBarCrossfade = findViewById(R.id.seekBarCrossfade);
        textCrossfadeDuration = findViewById(R.id.textCrossfadeDuration);
        switchVolumeNormalization = findViewById(R.id.switchVolumeNormalization);
        btnScanMedia = findViewById(R.id.btnScanMedia);
        textAppVersion = findViewById(R.id.textAppVersion);

        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            textAppVersion.setText(version);
        } catch (Exception e) {
            textAppVersion.setText("1.0.0");
        }
    }

    private void loadSettings() {
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);
//        switchDarkMode.setChecked(isDarkMode);

        boolean isCrossfade = preferences.getBoolean("crossfade_enabled", false);
        switchCrossfade.setChecked(isCrossfade);
        layoutCrossfadeDuration.setVisibility(isCrossfade ? View.VISIBLE : View.GONE);

        int crossfadeDuration = preferences.getInt("crossfade_duration", 3);
        seekBarCrossfade.setProgress(crossfadeDuration);
        textCrossfadeDuration.setText("Duration: " + crossfadeDuration + " seconds");

        boolean isVolumeNormalization = preferences.getBoolean("volume_normalization", false);
        switchVolumeNormalization.setChecked(isVolumeNormalization);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

//        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            preferences.edit().putBoolean("dark_mode", isChecked).apply();
//            AppCompatDelegate.setDefaultNightMode(
//                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
//        });

        switchCrossfade.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("crossfade_enabled", isChecked).apply();
            layoutCrossfadeDuration.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        seekBarCrossfade.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textCrossfadeDuration.setText("Duration: " + progress + " seconds");
                if (fromUser) {
                    preferences.edit().putInt("crossfade_duration", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchVolumeNormalization.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("volume_normalization", isChecked).apply();
        });

        btnScanMedia.setOnClickListener(v -> {
            viewModel.scanMediaFiles(this);
            android.widget.Toast.makeText(this, "Scanning media files...",
                    android.widget.Toast.LENGTH_SHORT).show();
        });
    }
}