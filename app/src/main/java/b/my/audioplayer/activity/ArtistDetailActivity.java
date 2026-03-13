package b.my.audioplayer.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import b.my.audioplayer.R;
import b.my.audioplayer.adapter.SongAdapter;
import b.my.audioplayer.viewmodel.MainViewModel;

public class ArtistDetailActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private RecyclerView songsRecyclerView;
    private SongAdapter songsAdapter;
    private String artistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        if (getIntent() != null) {
            artistName = getIntent().getStringExtra("artistName");
        }

        initViews();
        loadArtistSongs();
    }

    private void initViews() {
        songsRecyclerView = findViewById(R.id.recyclerPlaylistSongs);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songsAdapter = new SongAdapter(this);
        songsRecyclerView.setAdapter(songsAdapter);
    }

    private void loadArtistSongs() {
        if (artistName != null && !artistName.isEmpty()) {
            viewModel.getSongsByArtist(artistName).observe(this, songs -> {
                songsAdapter.setSongs(songs);
            });
        }
    }
}

