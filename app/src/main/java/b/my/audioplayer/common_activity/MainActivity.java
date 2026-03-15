package b.my.audioplayer.common_activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import b.my.audioplayer.R;
import b.my.audioplayer.audio_player.activity.AudioMainActivity;
import b.my.audioplayer.video_player.activity.VideoLibraryActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialCardView audioPlayerCard = findViewById(R.id.audioPlayerCard);
        MaterialCardView videoPlayerCard = findViewById(R.id.videoPlayerCard);

        audioPlayerCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AudioMainActivity.class);
            startActivity(intent);
        });

        videoPlayerCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VideoLibraryActivity.class);
            startActivity(intent);
        });
    }
}
