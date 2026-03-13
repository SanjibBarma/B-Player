package b.my.audioplayer.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import b.my.audioplayer.R;
import b.my.audioplayer.adapter.SongAdapter;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.viewmodel.MainViewModel;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextInputEditText searchInput;
    private LinearLayout emptyState;
    private ImageButton btnBack;

    private MusicPlaybackService musicService;
    private boolean isBound = false;
    private List<Song> searchResults = new ArrayList<>();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.MusicBinder binder = (MusicPlaybackService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        initViews();
        setupRecyclerView();
        setupSearch();
        bindService();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerSearchResults);
        searchInput = findViewById(R.id.searchInput);
        emptyState = findViewById(R.id.emptySearchState);
        btnBack = findViewById(R.id.btnBackSearch);

        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnSongClickListener((song, position) -> {
            if (isBound && !searchResults.isEmpty()) {
                musicService.getMusicPlayer().setPlaylist(searchResults, position);
                musicService.play();
                startActivity(new Intent(this, NowPlayingActivity.class));
            }
        });

        adapter.setOnSongMenuClickListener(new SongAdapter.OnSongMenuClickListener() {
            @Override
            public void onPlayNext(Song song) {
                if (isBound) {
                    musicService.getMusicPlayer().addToQueueNext(song);
                }
            }

            @Override
            public void onAddToPlaylist(Song song) {
                // Can be implemented if needed
            }

            @Override
            public void onToggleFavorite(Song song) {
                viewModel.toggleFavorite(song);
            }

            @Override
            public void onDelete(Song song) {
                viewModel.deleteSong(song);
            }

            @Override
            public void onShare(Song song) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("audio/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM,
                        android.net.Uri.parse("file://" + song.getPath()));
                startActivity(Intent.createChooser(shareIntent, "Share song"));
            }
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchInput.getText().toString());
                return true;
            }
            return false;
        });

        // Show keyboard
        searchInput.requestFocus();
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            adapter.setSongs(new ArrayList<>());
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        viewModel.searchSongs(query).observe(this, songs -> {
            searchResults = songs != null ? songs : new ArrayList<>();
            adapter.setSongs(searchResults);
            if (searchResults.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindService() {
        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }
}