package b.my.audioplayer.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

public class EqualizerViewModel extends AndroidViewModel {

    private MutableLiveData<short[]> bandLevels = new MutableLiveData<>();
    private MutableLiveData<Short> bassBoostStrength = new MutableLiveData<>((short) 0);
    private MutableLiveData<String> currentPreset = new MutableLiveData<>("Normal");
    private MutableLiveData<Boolean> isEqualizerEnabled = new MutableLiveData<>(false);

    private SharedPreferences preferences;

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
    }

    public void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("eq_enabled", Boolean.TRUE.equals(isEqualizerEnabled.getValue()));
        editor.putString("eq_preset", currentPreset.getValue());
        editor.putInt("bass_boost", bassBoostStrength.getValue());
        editor.apply();
    }

    public LiveData<short[]> getBandLevels() {
        return bandLevels;
    }

    public void setBandLevels(short[] levels) {
        bandLevels.setValue(levels);
    }

    public LiveData<Short> getBassBoostStrength() {
        return bassBoostStrength;
    }

    public void setBassBoostStrength(short strength) {
        bassBoostStrength.setValue(strength);
    }

    public LiveData<String> getCurrentPreset() {
        return currentPreset;
    }

    public void setCurrentPreset(String preset) {
        currentPreset.setValue(preset);
    }

    public LiveData<Boolean> getIsEqualizerEnabled() {
        return isEqualizerEnabled;
    }

    public void setEqualizerEnabled(boolean enabled) {
        isEqualizerEnabled.setValue(enabled);
    }
}