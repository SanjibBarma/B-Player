package b.my.audioplayer.audio_player.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.RemoteViews;
import b.my.audioplayer.R;
import b.my.audioplayer.audio_player.activity.AudioMainActivity;
import b.my.audioplayer.audio_player.service.MusicPlaybackService;
import b.my.audioplayer.utils.AlbumArtHelper;
import b.my.audioplayer.utils.Constants;

public class MusicWidgetProviderLarge extends AppWidgetProvider {

    private static final String TAG = "MusicWidgetLarge";
    public static final String ACTION_PLAY_PAUSE = "b.my.audioplayer.widget.PLAY_PAUSE_LARGE";
    public static final String ACTION_NEXT = "b.my.audioplayer.widget.NEXT_LARGE";
    public static final String ACTION_PREVIOUS = "b.my.audioplayer.widget.PREVIOUS_LARGE";
    public static final String ACTION_UPDATE_WIDGET = "b.my.audioplayer.widget.UPDATE_LARGE";

    private static String currentTitle = "No song playing";
    private static String currentArtist = "B Player";
    private static String currentAlbumArtPath = null;
    private static boolean isPlaying = false;
    private static long currentPosition = 0;
    private static long totalDuration = 0;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PLAY_PAUSE:
                sendActionToService(context, Constants.ACTION_TOGGLE_PLAYBACK);
                break;
            case ACTION_NEXT:
                sendActionToService(context, Constants.ACTION_NEXT);
                break;
            case ACTION_PREVIOUS:
                sendActionToService(context, Constants.ACTION_PREVIOUS);
                break;
            case ACTION_UPDATE_WIDGET:
                if (intent.hasExtra("title")) currentTitle = intent.getStringExtra("title");
                if (intent.hasExtra("artist")) currentArtist = intent.getStringExtra("artist");
                if (intent.hasExtra("album_art")) currentAlbumArtPath = intent.getStringExtra("album_art");
                if (intent.hasExtra("is_playing")) isPlaying = intent.getBooleanExtra("is_playing", false);
                currentPosition = intent.getLongExtra("position", 0);
                totalDuration = intent.getLongExtra("duration", 0);
                updateAllWidgets(context);
                break;
        }
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_music_player_large);

        // Set text
        remoteViews.setTextViewText(R.id.widgetLargeSongTitle, currentTitle);
        remoteViews.setTextViewText(R.id.widgetLargeArtistName, currentArtist);

        // Set album art
        if (currentAlbumArtPath != null) {
            Bitmap albumArt = AlbumArtHelper.getAlbumArtFromFile(currentAlbumArtPath);
            if (albumArt != null) {
                remoteViews.setImageViewBitmap(R.id.widgetLargeAlbumArt, albumArt);
            } else {
                remoteViews.setImageViewResource(R.id.widgetLargeAlbumArt, R.drawable.ic_music_note);
            }
        } else {
            remoteViews.setImageViewResource(R.id.widgetLargeAlbumArt, R.drawable.ic_music_note);
        }

        // Set play/pause button icon
        int playButtonRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        remoteViews.setImageViewResource(R.id.widgetLargeBtnPlayPause, playButtonRes);

        // Update Progress
        if (totalDuration > 0) {
            int progress = (int) ((currentPosition * 100) / totalDuration);
            remoteViews.setProgressBar(R.id.widgetLargeProgress, 100, progress, false);
        } else {
            remoteViews.setProgressBar(R.id.widgetLargeProgress, 100, 0, false);
        }

        // Set button intents
        setupClickIntents(context, remoteViews);

        appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }

    private static void setupClickIntents(Context context, RemoteViews views) {
        views.setOnClickPendingIntent(R.id.widgetLargeBtnPlayPause, getPendingIntent(context, ACTION_PLAY_PAUSE));
        views.setOnClickPendingIntent(R.id.widgetLargeBtnNext, getPendingIntent(context, ACTION_NEXT));
        views.setOnClickPendingIntent(R.id.widgetLargeBtnPrevious, getPendingIntent(context, ACTION_PREVIOUS));

        Intent mainIntent = new Intent(context, AudioMainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 10, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetLargeAlbumArt, mainPendingIntent);
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MusicWidgetProviderLarge.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        if (appWidgetIds != null) {
            for (int widgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId);
            }
        }
    }

    private static PendingIntent getPendingIntent(Context context, String action) {
        Intent intent = new Intent(context, MusicWidgetProviderLarge.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void sendActionToService(Context context, String action) {
        Intent intent = new Intent(context, MusicPlaybackService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void updateWidgetInfo(Context context, String title, String artist, String albumArt, boolean playing, long position, long duration) {
        currentTitle = title != null ? title : "No song playing";
        currentArtist = artist != null ? artist : "B Player";
        currentAlbumArtPath = albumArt;
        isPlaying = playing;
        currentPosition = position;
        totalDuration = duration;
        updateAllWidgets(context);
    }
}
