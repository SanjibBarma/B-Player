package b.my.audioplayer.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

public class EqualizerViewModel extends AndroidViewModel {

    private MutableLiveData<short[]> bandLevels = new MutableLiveData<>();
    private MutableLiveData<Short> bassBoostStrength = new MutableLiveData<>((short) 0);
    private MutableLiveData<Short> virtualizerStrength = new MutableLiveData<>((short) 0);
    private MutableLiveData<String> currentPreset = new MutableLiveData<>("Normal");
    private MutableLiveData<Boolean> isEqualizerEnabled = new MutableLiveData<>(false);

    private SharedPreferences preferences;

    private static final String KEY_BAND_PREFIX = "band_level_";
    private static final String KEY_BAND_COUNT = "band_count";

    public static final String[] PRESETS = {
            "Normal", "Rock", "Pop", "Classical", "Jazz", "Bass Boost", "Treble Boost"
    };

    public EqualizerViewModel(@NonNull Application application) {
        super(application);
        preferences = PreferenceManager.getDefaultSharedPreferences(application);
        loadSavedSettings();
    }

    private void loadSavedSettings() {
        isEqualizerEnabled.setValue(preferences.getBoolean("eq_enabled", false));
        currentPreset.setValue(preferences.getString("eq_preset", "Normal"));
        bassBoostStrength.setValue((short) preferences.getInt("bass_boost", 0));
        virtualizerStrength.setValue((short) preferences.getInt("virtualizer", 0));

        // Load band levels
        int bandCount = preferences.getInt(KEY_BAND_COUNT, 5);
        short[] levels = new short[bandCount];
        for (int i = 0; i < bandCount; i++) {
            levels[i] = (short) preferences.getInt(KEY_BAND_PREFIX + i, 0);
        }
        bandLevels.setValue(levels);
    }

    public void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("eq_enabled", Boolean.TRUE.equals(isEqualizerEnabled.getValue()));
        editor.putString("eq_preset", currentPreset.getValue());

        Short bass = bassBoostStrength.getValue();
        editor.putInt("bass_boost", bass != null ? bass : 0);

        Short virt = virtualizerStrength.getValue();
        editor.putInt("virtualizer", virt != null ? virt : 0);

        // Save band levels
        short[] levels = bandLevels.getValue();
        if (levels != null) {
            editor.putInt(KEY_BAND_COUNT, levels.length);
            for (int i = 0; i < levels.length; i++) {
                editor.putInt(KEY_BAND_PREFIX + i, levels[i]);
            }
        }

        editor.apply();
    }

    // ✅ নতুন method - Individual band level save
    public void saveBandLevel(int band, int level) {
        short[] levels = bandLevels.getValue();
        if (levels != null && band < levels.length) {
            levels[band] = (short) level;
            bandLevels.setValue(levels);
        }
        preferences.edit().putInt(KEY_BAND_PREFIX + band, level).apply();
    }

    // ✅ নতুন method - Get preset index
    public int getPresetIndex() {
        String preset = currentPreset.getValue();
        if (preset != null) {
            for (int i = 0; i < PRESETS.length; i++) {
                if (PRESETS[i].equals(preset)) {
                    return i;
                }
            }
        }
        return 0;
    }

    // Existing Getters
    public LiveData<short[]> getBandLevels() {
        return bandLevels;
    }

    public LiveData<Short> getBassBoostStrength() {
        return bassBoostStrength;
    }

    public LiveData<String> getCurrentPreset() {
        return currentPreset;
    }

    public LiveData<Boolean> getIsEqualizerEnabled() {
        return isEqualizerEnabled;
    }

    // ✅ নতুন Getter
    public LiveData<Short> getVirtualizerStrength() {
        return virtualizerStrength;
    }

    // Existing Setters
    public void setBandLevels(short[] levels) {
        bandLevels.setValue(levels);
    }

    public void setBassBoostStrength(short strength) {
        bassBoostStrength.setValue(strength);
    }

    public void setCurrentPreset(String preset) {
        currentPreset.setValue(preset);
    }

    public void setEqualizerEnabled(boolean enabled) {
        isEqualizerEnabled.setValue(enabled);
    }

    // ✅ নতুন Setter
    public void setVirtualizerStrength(short strength) {
        virtualizerStrength.setValue(strength);
    }
}