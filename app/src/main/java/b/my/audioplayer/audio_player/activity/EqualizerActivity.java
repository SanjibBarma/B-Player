package b.my.audioplayer.audio_player.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

import b.my.audioplayer.R;
import b.my.audioplayer.audio_player.service.MusicPlaybackService;
import b.my.audioplayer.audio_player.viewmodel.EqualizerViewModel;
import es.dmoral.toasty.Toasty;

public class EqualizerActivity extends AppCompatActivity {

    private static final String TAG = "EqualizerActivity";

    private EqualizerViewModel viewModel;
    private MusicPlaybackService musicService;
    private boolean isBound = false;

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;

    private SwitchMaterial switchEqualizer;
    private Spinner spinnerPresets;
    private LinearLayout equalizerBandsContainer;
    private SeekBar seekBarBassBoost;
    private SeekBar seekBarVirtualizer;
    private MaterialButton btnResetEqualizer;
    private ImageButton btnBack;

    private List<SeekBar> bandSeekBars = new ArrayList<>();
    private short minBandLevel = 0;
    private short maxBandLevel = 0;
    private boolean isInitialized = false;
    private boolean isUserInteraction = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.MusicBinder binder = (MusicPlaybackService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            initializeAudioEffects();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        viewModel = new ViewModelProvider(this).get(EqualizerViewModel.class);
        initViews();
        setupPresets();
        setupClickListeners();
        observeViewModel();
        bindService();
    }

    private void initViews() {
        switchEqualizer = findViewById(R.id.switchEqualizer);
        spinnerPresets = findViewById(R.id.spinnerPresets);
        equalizerBandsContainer = findViewById(R.id.equalizerBandsContainer);
        seekBarBassBoost = findViewById(R.id.seekBarBassBoost);
        seekBarVirtualizer = findViewById(R.id.seekBarVirtualizer);
        btnResetEqualizer = findViewById(R.id.btnResetEqualizer);
        btnBack = findViewById(R.id.btnBackEqualizer);
    }

    private void setupPresets() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, EqualizerViewModel.PRESETS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        switchEqualizer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUserInteraction) {
                viewModel.setEqualizerEnabled(isChecked);
                setEqualizerEnabled(isChecked);
                setControlsEnabled(isChecked);
            }
        });

        spinnerPresets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (isInitialized && isUserInteraction) {
                    String preset = EqualizerViewModel.PRESETS[position];
                    viewModel.setCurrentPreset(preset);
                    applyPreset(position);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        seekBarBassBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bassBoost != null) {
                    try {
                        bassBoost.setStrength((short) progress);
                        viewModel.setBassBoostStrength((short) progress);
                    } catch (Exception e) {
                        Log.e(TAG, "Bass boost error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserInteraction = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarVirtualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && virtualizer != null) {
                    try {
                        virtualizer.setStrength((short) progress);
                        viewModel.setVirtualizerStrength((short) progress);
                    } catch (Exception e) {
                        Log.e(TAG, "Virtualizer error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserInteraction = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnResetEqualizer.setOnClickListener(v -> resetEqualizer());
    }

    private void observeViewModel() {
        viewModel.getIsEqualizerEnabled().observe(this, isEnabled -> {
            if (!isUserInteraction) {
                switchEqualizer.setChecked(isEnabled);
                setControlsEnabled(isEnabled);
            }
        });

        viewModel.getBassBoostStrength().observe(this, strength -> {
            if (!isUserInteraction && strength != null) {
                seekBarBassBoost.setProgress(strength);
            }
        });

        viewModel.getVirtualizerStrength().observe(this, strength -> {
            if (!isUserInteraction && strength != null) {
                seekBarVirtualizer.setProgress(strength);
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializeAudioEffects() {
        if (!isBound || musicService == null) return;

        try {
            int audioSessionId = musicService.getMusicPlayer().getAudioSessionId();

            if (audioSessionId == 0) {
                Toasty.warning(this, "Play a song first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Initialize effects
            equalizer = new Equalizer(0, audioSessionId);
            bassBoost = new BassBoost(0, audioSessionId);
            virtualizer = new Virtualizer(0, audioSessionId);

            bassBoost.setEnabled(true);
            virtualizer.setEnabled(true);

            // Get band range
            short[] bandLevelRange = equalizer.getBandLevelRange();
            minBandLevel = bandLevelRange[0];
            maxBandLevel = bandLevelRange[1];

            // Setup UI
            setupEqualizerBands();
            applySavedSettings();

            isInitialized = true;

            runOnUiThread(() -> isUserInteraction = true);

        } catch (Exception e) {
            Log.e(TAG, "Init error: " + e.getMessage());
            Toasty.error(this, "Equalizer init failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupEqualizerBands() {
        if (equalizer == null) return;

        short numberOfBands = equalizer.getNumberOfBands();
        equalizerBandsContainer.removeAllViews();
        bandSeekBars.clear();

        for (short i = 0; i < numberOfBands; i++) {
            final short band = i;

            LinearLayout bandLayout = new LinearLayout(this);
            bandLayout.setOrientation(LinearLayout.VERTICAL);
            bandLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(4, 0, 4, 0);
            bandLayout.setLayoutParams(params);

            // + label
            TextView maxLabel = new TextView(this);
            maxLabel.setText("+");
            maxLabel.setTextSize(12);
            maxLabel.setGravity(Gravity.CENTER);
            maxLabel.setTextColor(getColor(R.color.colorSecondary));

            // SeekBar container
            LinearLayout seekBarContainer = new LinearLayout(this);
            seekBarContainer.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 150);
            seekBarContainer.setLayoutParams(containerParams);

            // Vertical SeekBar
            SeekBar bandSeekBar = new SeekBar(this);
            bandSeekBar.setMax(maxBandLevel - minBandLevel);
            bandSeekBar.setProgress(equalizer.getBandLevel(band) - minBandLevel);
            bandSeekBar.setRotation(270);
            LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(150, 40);
            bandSeekBar.setLayoutParams(seekParams);

            bandSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && equalizer != null) {
                        try {
                            short level = (short) (progress + minBandLevel);
                            equalizer.setBandLevel(band, level);
                            viewModel.saveBandLevel(band, progress);
                        } catch (Exception e) {
                            Log.e(TAG, "Band error: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isUserInteraction = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            // - label
            TextView minLabel = new TextView(this);
            minLabel.setText("-");
            minLabel.setTextSize(12);
            minLabel.setGravity(Gravity.CENTER);
            minLabel.setTextColor(getColor(R.color.colorSecondary));

            // Frequency label
            TextView freqLabel = new TextView(this);
            int freq = equalizer.getCenterFreq(band) / 1000;
            freqLabel.setText(freq >= 1000 ? (freq / 1000) + "K" : freq + "");
            freqLabel.setTextSize(10);
            freqLabel.setGravity(Gravity.CENTER);
            freqLabel.setTextColor(getColor(R.color.colorSecondary));

            seekBarContainer.addView(bandSeekBar);
            bandSeekBars.add(bandSeekBar);

            bandLayout.addView(maxLabel);
            bandLayout.addView(seekBarContainer);
            bandLayout.addView(minLabel);
            bandLayout.addView(freqLabel);

            equalizerBandsContainer.addView(bandLayout);
        }
    }

    private void applySavedSettings() {
        // Equalizer enabled
        Boolean isEnabled = viewModel.getIsEqualizerEnabled().getValue();
        if (isEnabled != null) {
            equalizer.setEnabled(isEnabled);
            switchEqualizer.setChecked(isEnabled);
            setControlsEnabled(isEnabled);
        }

        // Preset
        spinnerPresets.setSelection(viewModel.getPresetIndex());

        // Band levels
        short[] savedLevels = viewModel.getBandLevels().getValue();
        if (savedLevels != null && equalizer != null) {
            for (int i = 0; i < Math.min(savedLevels.length, bandSeekBars.size()); i++) {
                try {
                    equalizer.setBandLevel((short) i, savedLevels[i]);
                    bandSeekBars.get(i).setProgress(savedLevels[i] - minBandLevel);
                } catch (Exception e) {
                    Log.e(TAG, "Apply band error: " + e.getMessage());
                }
            }
        }

        // Bass boost
        Short bassStrength = viewModel.getBassBoostStrength().getValue();
        if (bassStrength != null && bassBoost != null) {
            try {
                bassBoost.setStrength(bassStrength);
                seekBarBassBoost.setProgress(bassStrength);
            } catch (Exception e) {
                Log.e(TAG, "Apply bass error: " + e.getMessage());
            }
        }

        // Virtualizer
        Short virtStrength = viewModel.getVirtualizerStrength().getValue();
        if (virtStrength != null && virtualizer != null) {
            try {
                virtualizer.setStrength(virtStrength);
                seekBarVirtualizer.setProgress(virtStrength);
            } catch (Exception e) {
                Log.e(TAG, "Apply virt error: " + e.getMessage());
            }
        }
    }

    private void setEqualizerEnabled(boolean enabled) {
        if (equalizer != null) {
            try {
                equalizer.setEnabled(enabled);
            } catch (Exception e) {
                Log.e(TAG, "Enable error: " + e.getMessage());
            }
        }
    }

    private void setControlsEnabled(boolean enabled) {
        spinnerPresets.setEnabled(enabled);
        seekBarBassBoost.setEnabled(enabled);
        seekBarVirtualizer.setEnabled(enabled);
        btnResetEqualizer.setEnabled(enabled);

        for (SeekBar seekBar : bandSeekBars) {
            seekBar.setEnabled(enabled);
        }

        float alpha = enabled ? 1.0f : 0.5f;
        equalizerBandsContainer.setAlpha(alpha);
        seekBarBassBoost.setAlpha(alpha);
        seekBarVirtualizer.setAlpha(alpha);
    }

    private void applyPreset(int presetIndex) {
        if (equalizer == null) return;

        try {
            short numPresets = equalizer.getNumberOfPresets();
            if (presetIndex < numPresets) {
                equalizer.usePreset((short) presetIndex);
                updateBandSeekBars();
            }
        } catch (Exception e) {
            Log.e(TAG, "Preset error: " + e.getMessage());
        }
    }

    private void updateBandSeekBars() {
        if (equalizer == null) return;

        for (int i = 0; i < bandSeekBars.size(); i++) {
            try {
                short level = equalizer.getBandLevel((short) i);
                int progress = level - minBandLevel;
                bandSeekBars.get(i).setProgress(progress);
                viewModel.saveBandLevel(i, progress);
            } catch (Exception e) {
                Log.e(TAG, "Update band error: " + e.getMessage());
            }
        }
    }

    private void resetEqualizer() {
        // Reset bands
        if (equalizer != null) {
            short centerLevel = (short) ((maxBandLevel + minBandLevel) / 2);
            int centerProgress = centerLevel - minBandLevel;

            for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
                try {
                    equalizer.setBandLevel(i, centerLevel);
                    if (i < bandSeekBars.size()) {
                        bandSeekBars.get(i).setProgress(centerProgress);
                    }
                    viewModel.saveBandLevel(i, centerProgress);
                } catch (Exception e) {
                    Log.e(TAG, "Reset band error: " + e.getMessage());
                }
            }
        }

        // Reset bass
        if (bassBoost != null) {
            try {
                bassBoost.setStrength((short) 0);
                seekBarBassBoost.setProgress(0);
                viewModel.setBassBoostStrength((short) 0);
            } catch (Exception e) {
                Log.e(TAG, "Reset bass error: " + e.getMessage());
            }
        }

        // Reset virtualizer
        if (virtualizer != null) {
            try {
                virtualizer.setStrength((short) 0);
                seekBarVirtualizer.setProgress(0);
                viewModel.setVirtualizerStrength((short) 0);
            } catch (Exception e) {
                Log.e(TAG, "Reset virt error: " + e.getMessage());
            }
        }

        spinnerPresets.setSelection(0);
        viewModel.setCurrentPreset(EqualizerViewModel.PRESETS[0]);

        Toasty.success(this, "Reset to default", Toast.LENGTH_SHORT).show();
    }

    private void bindService() {
        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.saveSettings();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}