package b.my.audioplayer;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import b.my.audioplayer.utils.Constants;

public class BPlayerApplication extends Application {

    private static BPlayerApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Apply theme
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = preferences.getBoolean(Constants.PREF_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static BPlayerApplication getInstance() {
        return instance;
    }
}