package b.my.audioplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import b.my.audioplayer.R;
import b.my.audioplayer.activity.MainActivity;
import b.my.audioplayer.activity.NowPlayingActivity;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.utils.AlbumArtHelper;
import b.my.audioplayer.utils.Constants;

public class MusicWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_PLAY_PAUSE = "b.my.audioplayer.widget.PLAY_PAUSE";
    public static final String ACTION_NEXT = "b.my.audioplayer.widget.NEXT";
    public static final String ACTION_PREVIOUS = "b.my.audioplayer.widget.PREVIOUS";
    public static final String ACTION_UPDATE_WIDGET = "b.my.audioplayer.widget.UPDATE";

    private static String currentTitle = "No song playing";
    private static String currentArtist = "B Player";
    private static String currentAlbumArtPath = null;
    private static boolean isPlaying = false;

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
                // Get updated info from intent extras
                if (intent.hasExtra("title")) {
                    currentTitle = intent.getStringExtra("title");
                }
                if (intent.hasExtra("artist")) {
                    currentArtist = intent.getStringExtra("artist");
                }
                if (intent.hasExtra("album_art")) {
                    currentAlbumArtPath = intent.getStringExtra("album_art");
                }
                if (intent.hasExtra("is_playing")) {
                    isPlaying = intent.getBooleanExtra("is_playing", false);
                }
                updateAllWidgets(context);
                break;
        }
    }

    private void sendActionToService(Context context, String action) {
        Intent serviceIntent = new Intent(context, MusicPlaybackService.class);
        serviceIntent.setAction(action);
        context.startService(serviceIntent);
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, MusicWidgetProvider.class)
        );

        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_music_player);

        // Set song info
        views.setTextViewText(R.id.widgetSongTitle, currentTitle);
        views.setTextViewText(R.id.widgetArtistName, currentArtist);

        // Set album art
        if (currentAlbumArtPath != null) {
            Bitmap albumArt = AlbumArtHelper.getAlbumArtFromFile(currentAlbumArtPath);
            if (albumArt != null) {
                views.setImageViewBitmap(R.id.widgetAlbumArt, albumArt);
            } else {
                views.setImageViewResource(R.id.widgetAlbumArt, R.drawable.ic_music_note);
            }
        } else {
            views.setImageViewResource(R.id.widgetAlbumArt, R.drawable.ic_music_note);
        }

        // Set play/pause icon
        views.setImageViewResource(R.id.widgetBtnPlayPause,
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

        // Set click intents
        // Play/Pause
        Intent playPauseIntent = new Intent(context, MusicWidgetProvider.class);
        playPauseIntent.setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(
                context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetBtnPlayPause, playPausePendingIntent);

        // Next
        Intent nextIntent = new Intent(context, MusicWidgetProvider.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(
                context, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetBtnNext, nextPendingIntent);

        // Previous
        Intent previousIntent = new Intent(context, MusicWidgetProvider.class);
        previousIntent.setAction(ACTION_PREVIOUS);
        PendingIntent previousPendingIntent = PendingIntent.getBroadcast(
                context, 2, previousIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetBtnPrevious, previousPendingIntent);

        // Open app on album art click
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context, 3, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetAlbumArt, openAppPendingIntent);

        // Open Now Playing on title click
        Intent openNowPlayingIntent = new Intent(context, NowPlayingActivity.class);
        PendingIntent openNowPlayingPendingIntent = PendingIntent.getActivity(
                context, 4, openNowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetSongTitle, openNowPlayingPendingIntent);
        views.setOnClickPendingIntent(R.id.widgetArtistName, openNowPlayingPendingIntent);

        appWidgetManager.updateAppWidget(widgetId, views);
    }

    public static void updateWidgetInfo(Context context, String title, String artist,
                                        String albumArtPath, boolean playing) {
        currentTitle = title != null ? title : "No song playing";
        currentArtist = artist != null ? artist : "B Player";
        currentAlbumArtPath = albumArtPath;
        isPlaying = playing;
        updateAllWidgets(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }
}