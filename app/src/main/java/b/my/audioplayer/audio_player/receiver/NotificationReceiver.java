package b.my.audioplayer.audio_player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import b.my.audioplayer.audio_player.service.MusicPlaybackService;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        Intent serviceIntent = new Intent(context, MusicPlaybackService.class);
        serviceIntent.setAction(action);
        context.startService(serviceIntent);
    }
}