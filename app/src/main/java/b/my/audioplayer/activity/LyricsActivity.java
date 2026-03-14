package b.my.audioplayer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import b.my.audioplayer.R;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.Constants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LyricsActivity extends AppCompatActivity {

    private TextView songTitle;
    private TextView artistName;
    private TextView lyricsContent;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private ImageButton btnSearch;

    private Song currentSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);

        // Try to get song object from Intent
        currentSong = (Song) getIntent().getSerializableExtra(Constants.EXTRA_SONG);
        
        if (currentSong == null) {
            // Fallback if song object is not found
            finish();
            return;
        }

        initViews();
        loadLyrics();
    }

    private void initViews() {
        songTitle = findViewById(R.id.lyricsSongTitle);
        artistName = findViewById(R.id.lyricsArtistName);
        lyricsContent = findViewById(R.id.lyricsContent);
        progressBar = findViewById(R.id.lyricsProgress);
        btnBack = findViewById(R.id.btnBackLyrics);
        btnSearch = findViewById(R.id.btnSearchLyrics);

        songTitle.setText(currentSong.getTitle());
        artistName.setText(currentSong.getArtist());

        songTitle.setSelected(true);
        artistName.setSelected(true);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnSearch.setOnClickListener(v -> searchLyricsOnline());
    }

    private void loadLyrics() {
        progressBar.setVisibility(View.VISIBLE);
        lyricsContent.setVisibility(View.GONE);

        // Try to load .lrc file based on song path
        if (currentSong.getPath() != null) {
            String lrcPath = currentSong.getPath().replaceAll("\\.[^.]+$", ".lrc");
            File lrcFile = new File(lrcPath);

            if (lrcFile.exists()) {
                loadLyricsFromFile(lrcFile);
                return;
            }
        }
        
        // If no file found, show default message
        progressBar.setVisibility(View.GONE);
        lyricsContent.setVisibility(View.VISIBLE);
        lyricsContent.setText("No lyrics available\n\nTap the search icon to find lyrics online");
    }

    private void loadLyricsFromFile(File lrcFile) {
        new Thread(() -> {
            StringBuilder lyrics = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(lrcFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Remove timestamps from LRC format [mm:ss.xx]
                    line = line.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]", "").trim();
                    if (!line.isEmpty() && !line.startsWith("[")) {
                        lyrics.append(line).append("\n\n");
                    }
                }
                reader.close();

                String finalLyrics = lyrics.toString().trim();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    lyricsContent.setVisibility(View.VISIBLE);
                    if (finalLyrics.isEmpty()) {
                        lyricsContent.setText("No lyrics available");
                    } else {
                        lyricsContent.setText(finalLyrics);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    lyricsContent.setVisibility(View.VISIBLE);
                    lyricsContent.setText("Failed to load lyrics");
                });
            }
        }).start();
    }

    private void searchLyricsOnline() {
        String query = currentSong.getTitle() + " " + currentSong.getArtist() + " lyrics";
        String url = "https://www.google.com/search?q=" + android.net.Uri.encode(query);
        android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
        );
        startActivity(intent);
    }
}