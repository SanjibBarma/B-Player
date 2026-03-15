package b.my.audioplayer.common_activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import b.my.audioplayer.R;
import b.my.audioplayer.video_player.activity.VideoLibraryActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        VideoView videoView = findViewById(R.id.videoView);

        String path = "android.resource://" + getPackageName() + "/" + R.raw.ic_app_logo;
        Uri uri = Uri.parse(path);

        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(mp -> {
            mp.setVolume(0f, 0f);   // sound mute
            //mp.setLooping(true);
        });

        videoView.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//            Intent intent = new Intent(SplashActivity.this, AudioMainActivity.class);
//            Intent intent = new Intent(SplashActivity.this, VideoLibraryActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
