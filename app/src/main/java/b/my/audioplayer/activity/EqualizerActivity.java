package b.my.audioplayer.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import b.my.audioplayer.R;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.viewmodel.EqualizerViewModel;
import java.util.ArrayList;
import java.util.List;

public class EqualizerActivity extends AppCompatActivity {

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

    private ServiceConnection serviceConnection = new ServiceConnection() {
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
        btnBack.setOnClickListener(v -> onBackPressed());

        switchEqualizer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setEqualizerEnabled(isChecked);
            setEqualizerEnabled(isChecked);
        });

        spinnerPresets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String preset = EqualizerViewModel.PRESETS[position];
                viewModel.setCurrentPreset(preset);
                applyPreset(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        seekBarBassBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bassBoost != null) {
                    bassBoost.setStrength((short) progress);
                    viewModel.setBassBoostStrength((short) progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarVirtualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && virtualizer != null) {
                    virtualizer.setStrength((short) progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnResetEqualizer.setOnClickListener(v -> resetEqualizer());
    }

    private void observeViewModel() {
        viewModel.getIsEqualizerEnabled().observe(this, isEnabled -> {
            switchEqualizer.setChecked(isEnabled);
        });

        viewModel.getBassBoostStrength().observe(this, strength -> {
            seekBarBassBoost.setProgress(strength);
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializeAudioEffects() {
        if (!isBound) return;

        int audioSessionId = musicService.getMusicPlayer().getExoPlayer().getAudioSessionId();

        try {
            equalizer = new Equalizer(0, audioSessionId);
            bassBoost = new BassBoost(0, audioSessionId);
            virtualizer = new Virtualizer(0, audioSessionId);

            equalizer.setEnabled(Boolean.TRUE.equals(viewModel.getIsEqualizerEnabled().getValue()));
            bassBoost.setEnabled(true);
            virtualizer.setEnabled(true);

            setupEqualizerBands();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupEqualizerBands() {
        if (equalizer == null) return;

        short numberOfBands = equalizer.getNumberOfBands();
        short[] bandLevelRange = equalizer.getBandLevelRange();
        short minLevel = bandLevelRange[0];
        short maxLevel = bandLevelRange[1];

        equalizerBandsContainer.removeAllViews();
        bandSeekBars.clear();

        for (short i = 0; i < numberOfBands; i++) {
            final short band = i;

            LinearLayout bandLayout = new LinearLayout(this);
            bandLayout.setOrientation(LinearLayout.VERTICAL);
            bandLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(8, 0, 8, 0);
            bandLayout.setLayoutParams(params);

            // Frequency label
            TextView freqLabel = new TextView(this);
            int freq = equalizer.getCenterFreq(band) / 1000;
            freqLabel.setText(freq >= 1000 ? (freq / 1000) + "K" : freq + "Hz");
            freqLabel.setTextSize(10);
            freqLabel.setGravity(Gravity.CENTER);

            // Vertical SeekBar
            SeekBar bandSeekBar = new SeekBar(this);
            bandSeekBar.setMax(maxLevel - minLevel);
            bandSeekBar.setProgress(equalizer.getBandLevel(band) - minLevel);
            bandSeekBar.setRotation(270);
            LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                    150, 40);
            bandSeekBar.setLayoutParams(seekParams);

            bandSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && equalizer != null) {
                        equalizer.setBandLevel(band, (short) (progress + minLevel));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            bandSeekBars.add(bandSeekBar);
            bandLayout.addView(bandSeekBar);
            bandLayout.addView(freqLabel);
            equalizerBandsContainer.addView(bandLayout);
        }
    }

    private void setEqualizerEnabled(boolean enabled) {
        if (equalizer != null) {
            equalizer.setEnabled(enabled);
        }
    }

    private void applyPreset(int presetIndex) {
        if (equalizer == null) return;

        try {
            if (presetIndex < equalizer.getNumberOfPresets()) {
                equalizer.usePreset((short) presetIndex);
                updateBandSeekBars();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBandSeekBars() {
        if (equalizer == null) return;

        short[] bandLevelRange = equalizer.getBandLevelRange();
        short minLevel = bandLevelRange[0];

        for (int i = 0; i < bandSeekBars.size(); i++) {
            short level = equalizer.getBandLevel((short) i);
            bandSeekBars.get(i).setProgress(level - minLevel);
        }
    }

    private void resetEqualizer() {
        if (equalizer != null) {
            short[] bandLevelRange = equalizer.getBandLevelRange();
            short minLevel = bandLevelRange[0];
            short maxLevel = bandLevelRange[1];
            short centerLevel = (short) ((maxLevel + minLevel) / 2);

            for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
                equalizer.setBandLevel(i, centerLevel);
            }
            updateBandSeekBars();
        }

        if (bassBoost != null) {
            bassBoost.setStrength((short) 0);
            seekBarBassBoost.setProgress(0);
        }

        if (virtualizer != null) {
            virtualizer.setStrength((short) 0);
            seekBarVirtualizer.setProgress(0);
        }

        spinnerPresets.setSelection(0);
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
        }
    }
}