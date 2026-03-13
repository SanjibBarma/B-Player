package b.my.audioplayer.utils;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.widget.TextView;

public class Constants {

    // Shared Preferences Keys
    public static final String PREF_NAME = "BPlayerPrefs";
    public static final String PREF_DARK_MODE = "dark_mode";
    public static final String PREF_CROSSFADE_ENABLED = "crossfade_enabled";
    public static final String PREF_CROSSFADE_DURATION = "crossfade_duration";
    public static final String PREF_VOLUME_NORMALIZATION = "volume_normalization";
    public static final String PREF_LAST_PLAYED_SONG_ID = "last_played_song_id";
    public static final String PREF_LAST_POSITION = "last_position";
    public static final String PREF_SHUFFLE_ENABLED = "shuffle_enabled";
    public static final String PREF_REPEAT_MODE = "repeat_mode";

    // Intent Actions
    public static final String ACTION_PLAY = "b.my.audioplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "b.my.audioplayer.ACTION_PAUSE";
    public static final String ACTION_NEXT = "b.my.audioplayer.ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "b.my.audioplayer.ACTION_PREVIOUS";
    public static final String ACTION_STOP = "b.my.audioplayer.ACTION_STOP";
    public static final String ACTION_TOGGLE_PLAYBACK = "b.my.audioplayer.ACTION_TOGGLE";

    // Notification
    public static final String CHANNEL_ID = "BPlayerChannel";
    public static final int NOTIFICATION_ID = 1;

    // Widget
    public static final String WIDGET_ACTION_PLAY_PAUSE = "b.my.audioplayer.widget.PLAY_PAUSE";
    public static final String WIDGET_ACTION_NEXT = "b.my.audioplayer.widget.NEXT";
    public static final String WIDGET_ACTION_PREVIOUS = "b.my.audioplayer.widget.PREVIOUS";

    // Bundle Keys
    public static final String EXTRA_SONG = "extra_song";
    public static final String EXTRA_SONG_LIST = "extra_song_list";
    public static final String EXTRA_POSITION = "extra_position";
    public static final String EXTRA_PLAYLIST_ID = "extra_playlist_id";
    public static final String EXTRA_ALBUM_ID = "extra_album_id";
    public static final String EXTRA_ALBUM_NAME = "extra_album_name";
    public static final String EXTRA_ARTIST_ID = "extra_artist_id";
    public static final String EXTRA_ARTIST_NAME = "extra_artist_name";

    // Sort Options
    public static final int SORT_BY_TITLE = 0;
    public static final int SORT_BY_ARTIST = 1;
    public static final int SORT_BY_ALBUM = 2;
    public static final int SORT_BY_DATE = 3;
    public static final int SORT_BY_DURATION = 4;

    // Audio Formats
    public static final String[] SUPPORTED_FORMATS = {
            ".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a", ".wma", ".alac"
    };


    public static void textGradient(TextView textView) {
        Paint paint = textView.getPaint();
        float width = paint.measureText(textView.getText().toString());

        Shader shader = new LinearGradient(
                0, 0, width, textView.getTextSize(),
                new int[]{
                        Color.WHITE,
                        Color.YELLOW,
                        Color.parseColor("#D0964B")
                },
                null,
                Shader.TileMode.CLAMP
        );
        textView.getPaint().setShader(shader);
    }
}