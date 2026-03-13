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

public class MusicWidgetProviderLarge extends AppWidgetProvider {

    public static final String ACTION_PLAY_PAUSE = "b.my.audioplayer.widget.PLAY_PAUSE_LARGE";
    public static final String ACTION_NEXT = "b.my.audioplayer.widget.NEXT_LARGE";
    public static final String ACTION_PREVIOUS = "b.my.audioplayer.widget.PREVIOUS_LARGE";
    public static final String ACTION_UPDATE_WIDGET = "b.my.audioplayer.widget.UPDATE_LARGE";

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
                currentTitle = intent.getStringExtra("title");
                currentArtist = intent.getStringExtra("artist");
                currentAlbumArtPath = intent.getStringExtra("albumArt");
                isPlaying = intent.getBooleanExtra("isPlaying", false);
                updateAllWidgets(context);
                break;
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_music_player_large);

        // Set text
        remoteViews.setTextViewText(R.id.widgetLargeSongTitle, currentTitle);
        remoteViews.setTextViewText(R.id.widgetLargeArtistName, currentArtist);

        // Set album art
        if (currentAlbumArtPath != null) {
            try {
                Bitmap albumArt = AlbumArtHelper.getAlbumArtFromFile(currentAlbumArtPath);
                if (albumArt != null) {
                    remoteViews.setImageViewBitmap(R.id.widgetLargeAlbumArt, albumArt);
                }
            } catch (Exception e) {
                remoteViews.setImageViewResource(R.id.widgetLargeAlbumArt, R.drawable.ic_music_note);
            }
        } else {
            remoteViews.setImageViewResource(R.id.widgetLargeAlbumArt, R.drawable.ic_music_note);
        }

        // Set play/pause button
        int playButtonRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        remoteViews.setImageViewResource(R.id.widgetLargeBtnPlayPause, playButtonRes);

        // Set button intents
        remoteViews.setOnClickPendingIntent(R.id.widgetLargeBtnPlayPause, getPendingIntent(context, ACTION_PLAY_PAUSE));
        remoteViews.setOnClickPendingIntent(R.id.widgetLargeBtnNext, getPendingIntent(context, ACTION_NEXT));
        remoteViews.setOnClickPendingIntent(R.id.widgetLargeBtnPrevious, getPendingIntent(context, ACTION_PREVIOUS));

        // Set click intent for opening the app
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        remoteViews.setOnClickPendingIntent(R.id.widgetLargeAlbumArt, mainPendingIntent);

        appWidgetManager.updateAppWidget(widgetId, remoteViews);
    }

    private void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MusicWidgetProviderLarge.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    private PendingIntent getPendingIntent(Context context, String action) {
        Intent intent = new Intent(context, MusicWidgetProviderLarge.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void sendActionToService(Context context, String action) {
        Intent intent = new Intent(context, MusicPlaybackService.class);
        intent.setAction(action);
        context.startService(intent);
    }
}

