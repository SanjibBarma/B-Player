package b.my.audioplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class PreferenceHelper {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public PreferenceHelper(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
    }

    // Dark Mode
    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(Constants.PREF_DARK_MODE, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        editor.putBoolean(Constants.PREF_DARK_MODE, enabled).apply();
    }

    // Crossfade
    public boolean isCrossfadeEnabled() {
        return preferences.getBoolean(Constants.PREF_CROSSFADE_ENABLED, false);
    }

    public void setCrossfadeEnabled(boolean enabled) {
        editor.putBoolean(Constants.PREF_CROSSFADE_ENABLED, enabled).apply();
    }

    public int getCrossfadeDuration() {
        return preferences.getInt(Constants.PREF_CROSSFADE_DURATION, 3);
    }

    public void setCrossfadeDuration(int duration) {
        editor.putInt(Constants.PREF_CROSSFADE_DURATION, duration).apply();
    }

    // Volume Normalization
    public boolean isVolumeNormalizationEnabled() {
        return preferences.getBoolean(Constants.PREF_VOLUME_NORMALIZATION, false);
    }

    public void setVolumeNormalizationEnabled(boolean enabled) {
        editor.putBoolean(Constants.PREF_VOLUME_NORMALIZATION, enabled).apply();
    }

    // Last Played Song
    public long getLastPlayedSongId() {
        return preferences.getLong(Constants.PREF_LAST_PLAYED_SONG_ID, -1);
    }

    public void setLastPlayedSongId(long songId) {
        editor.putLong(Constants.PREF_LAST_PLAYED_SONG_ID, songId).apply();
    }

    // Last Position
    public long getLastPosition() {
        return preferences.getLong(Constants.PREF_LAST_POSITION, 0);
    }

    public void setLastPosition(long position) {
        editor.putLong(Constants.PREF_LAST_POSITION, position).apply();
    }

    // Shuffle
    public boolean isShuffleEnabled() {
        return preferences.getBoolean(Constants.PREF_SHUFFLE_ENABLED, false);
    }

    public void setShuffleEnabled(boolean enabled) {
        editor.putBoolean(Constants.PREF_SHUFFLE_ENABLED, enabled).apply();
    }

    // Repeat Mode
    public int getRepeatMode() {
        return preferences.getInt(Constants.PREF_REPEAT_MODE, 0);
    }

    public void setRepeatMode(int mode) {
        editor.putInt(Constants.PREF_REPEAT_MODE, mode).apply();
    }
}