package b.my.audioplayer.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;

import b.my.audioplayer.R;
import b.my.audioplayer.adapter.SongAdapter;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.utils.Constants;
import b.my.audioplayer.viewmodel.MainViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.dmoral.toasty.Toasty;

public class AlbumDetailActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private ImageView albumCoverArt;
    private CollapsingToolbarLayout collapsingToolbar;
    private Toolbar toolbar;
    private MaterialButton btnPlayAll, btnShuffleAll;
    private View emptyStateView;

    private long albumId;
    private String albumName;
    private List<Song> albumSongs = new ArrayList<>();

    private MusicPlaybackService musicService;
    private boolean isBound = false;

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
        setContentView(R.layout.activity_playlist_detail);

        albumId = getIntent().getLongExtra(Constants.EXTRA_ALBUM_ID, -1);
        albumName = getIntent().getStringExtra(Constants.EXTRA_ALBUM_NAME);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        setupToolbar();
        setupRecyclerView();
        observeData();
        bindMusicService();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerPlaylistSongs);
        albumCoverArt = findViewById(R.id.playlistCoverArt);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        toolbar = findViewById(R.id.toolbar);
        btnPlayAll = findViewById(R.id.btnPlayAll);
        btnShuffleAll = findViewById(R.id.btnShuffleAll);
        emptyStateView = findViewById(R.id.emptyStateView);

        findViewById(R.id.fabAddSongs).setVisibility(View.GONE);

        collapsingToolbar.setTitle(albumName != null ? albumName : "Album");
        collapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, R.color.colorSecondary));
        collapsingToolbar.setCollapsedTitleTextColor(ContextCompat.getColor(this, R.color.colorSecondary));

        btnPlayAll.setOnClickListener(v -> playAll());
        btnShuffleAll.setOnClickListener(v -> shuffleAll());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnSongClickListener((song, position) -> {
            if (isBound) {
                musicService.getMusicPlayer().setPlaylist(albumSongs, position);
                musicService.play();
                startActivity(new Intent(this, NowPlayingActivity.class));
            }
        });

        adapter.setOnSongMenuClickListener(new SongAdapter.OnSongMenuClickListener() {
            @Override
            public void onPlayNext(Song song) {
                if (isBound) musicService.getMusicPlayer().addToQueueNext(song);
            }

            @Override
            public void onAddToPlaylist(Song song) {
                // Implement if needed
            }

            @Override
            public void onToggleFavorite(Song song) {
                viewModel.toggleFavorite(song);
            }

            @Override
            public void onDelete(Song song) {
                // Albums usually shouldn't delete individual songs from here easily
            }

            @Override
            public void onShare(Song song) {
                shareSong(song);
            }
        });
    }

    private void observeData() {
        if (albumId != -1) {
            // This now handles both normal albums and Ringtones virtual album
            viewModel.getSongsByAlbum(albumId).observe(this, songs -> {
                albumSongs = songs != null ? songs : new ArrayList<>();
                adapter.setSongs(albumSongs);

                if (albumSongs.isEmpty()) {
                    emptyStateView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    // Set album art
                    if (albumId == Constants.RINGTONE_ALBUM_ID) {
                        // Use default ringtone icon for Ringtones album
                        albumCoverArt.setImageResource(R.drawable.ic_ringtone);
                    } else if (albumSongs.get(0).getAlbumArt() != null) {
                        Glide.with(this)
                                .load(albumSongs.get(0).getAlbumArt())
                                .placeholder(R.drawable.ic_music_note)
                                .into(albumCoverArt);
                    }
                }
            });
        }
    }

    private void playAll() {
        if (isBound && !albumSongs.isEmpty()) {
            musicService.getMusicPlayer().setPlaylist(albumSongs, 0);
            musicService.play();
            startActivity(new Intent(this, NowPlayingActivity.class));
        }
    }

    private void shuffleAll() {
        if (isBound && !albumSongs.isEmpty()) {
            List<Song> shuffledList = new ArrayList<>(albumSongs);
            Collections.shuffle(shuffledList);
            musicService.getMusicPlayer().setPlaylist(shuffledList, 0);
            musicService.play();
            startActivity(new Intent(this, NowPlayingActivity.class));
        }
    }

    private void shareSong(Song song) {
        if (song == null || song.getPath() == null) {
            Toasty.error(this, "Cannot share this song", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = new File(song.getPath());

            if (!file.exists()) {
                Toasty.error(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    "b.my.audioplayer.fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share \"" + song.getTitle() + "\""));

        } catch (IllegalArgumentException e) {
            Toasty.error(this, "Cannot share this file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toasty.error(this, "Error sharing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }
}