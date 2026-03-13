package b.my.audioplayer.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import b.my.audioplayer.R;
import b.my.audioplayer.adapter.SongAdapter;
import b.my.audioplayer.model.Playlist;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.viewmodel.PlaylistViewModel;
import b.my.audioplayer.utils.Constants;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private PlaylistViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView playlistCoverArt;
    private MaterialButton btnPlayAll;
    private MaterialButton btnShuffleAll;
    private FloatingActionButton fabAddSongs;
    private Toolbar toolbar;
    private TextView emptyStateText;
    private View emptyStateView;

    private MusicPlaybackService musicService;
    private boolean isBound = false;

    private Playlist currentPlaylist;
    private long playlistId;
    private List<Song> playlistSongs = new ArrayList<>();

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

        playlistId = getIntent().getLongExtra(Constants.EXTRA_PLAYLIST_ID, -1);
        if (playlistId == -1) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(PlaylistViewModel.class);
        initViews();
        setupRecyclerView();
        loadPlaylistDetails();
        loadPlaylistSongs();
        bindService();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        playlistCoverArt = findViewById(R.id.playlistCoverArt);
        btnPlayAll = findViewById(R.id.btnPlayAll);
        btnShuffleAll = findViewById(R.id.btnShuffleAll);
        fabAddSongs = findViewById(R.id.fabAddSongs);
        recyclerView = findViewById(R.id.recyclerPlaylistSongs);
        emptyStateView = findViewById(R.id.emptyStateView);
        emptyStateText = findViewById(R.id.emptyStateText);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        btnPlayAll.setOnClickListener(v -> playAll(false));
        btnShuffleAll.setOnClickListener(v -> playAll(true));
        fabAddSongs.setOnClickListener(v -> showAddSongsDialog());
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnSongClickListener((song, position) -> {
            if (isBound && !playlistSongs.isEmpty()) {
                musicService.getMusicPlayer().setPlaylist(playlistSongs, position);
                musicService.play();
                startActivity(new Intent(this, NowPlayingActivity.class));
            }
        });

        adapter.setOnSongMenuClickListener(new SongAdapter.OnSongMenuClickListener() {
            @Override
            public void onPlayNext(Song song) {
                if (isBound) {
                    musicService.getMusicPlayer().addToQueueNext(song);
                    Toast.makeText(PlaylistDetailActivity.this,
                            "Will play next", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAddToPlaylist(Song song) {
                showAddToPlaylistDialog(song);
            }

            @Override
            public void onToggleFavorite(Song song) {
                // Toggle favorite in main ViewModel
                Toast.makeText(PlaylistDetailActivity.this,
                        song.isFavorite() ? "Removed from favorites" : "Added to favorites",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(Song song) {
                removeFromPlaylist(song);
            }

            @Override
            public void onShare(Song song) {
                shareSong(song);
            }
        });

        // Setup swipe to remove
        setupSwipeToRemove();
    }

    private void setupSwipeToRemove() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < playlistSongs.size()) {
                    Song song = playlistSongs.get(position);
                    removeFromPlaylist(song);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void loadPlaylistDetails() {
        viewModel.getPlaylistById(playlistId).observe(this, playlist -> {
            if (playlist != null) {
                currentPlaylist = playlist;
                collapsingToolbar.setTitle(playlist.getName());

                // Load cover art if available
                if (playlist.getCoverArtPath() != null) {
                    Glide.with(this)
                            .load(playlist.getCoverArtPath())
                            .placeholder(R.drawable.ic_playlist)
                            .error(R.drawable.ic_playlist)
                            .into(playlistCoverArt);
                }
            }
        });
    }

    private void loadPlaylistSongs() {
        viewModel.getPlaylistSongs(playlistId).observe(this, songs -> {
            playlistSongs = songs != null ? songs : new ArrayList<>();
            adapter.setSongs(playlistSongs);

            // Show/hide empty state
            if (playlistSongs.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                if (emptyStateView != null) emptyStateView.setVisibility(View.VISIBLE);
                btnPlayAll.setEnabled(false);
                btnShuffleAll.setEnabled(false);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                if (emptyStateView != null) emptyStateView.setVisibility(View.GONE);
                btnPlayAll.setEnabled(true);
                btnShuffleAll.setEnabled(true);

                // Update cover art with first song's album art
                if (playlistSongs.get(0).getAlbumArt() != null) {
                    Glide.with(this)
                            .load(playlistSongs.get(0).getAlbumArt())
                            .placeholder(R.drawable.ic_playlist)
                            .into(playlistCoverArt);
                }
            }
        });
    }

    private void playAll(boolean shuffle) {
        if (isBound && !playlistSongs.isEmpty()) {
            if (shuffle && !musicService.getMusicPlayer().isShuffleEnabled()) {
                musicService.getMusicPlayer().toggleShuffle();
            } else if (!shuffle && musicService.getMusicPlayer().isShuffleEnabled()) {
                musicService.getMusicPlayer().toggleShuffle();
            }

            musicService.getMusicPlayer().setPlaylist(playlistSongs, 0);
            musicService.play();
            startActivity(new Intent(this, NowPlayingActivity.class));
        } else if (playlistSongs.isEmpty()) {
            Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddSongsDialog() {
        Intent intent = new Intent(this, SongPickerActivity.class);
        intent.putExtra(Constants.EXTRA_PLAYLIST_ID, playlistId);
        startActivity(intent);
    }

    private void showAddToPlaylistDialog(Song song) {
        // Show dialog with all playlists
        viewModel.getAllPlaylists().observe(this, playlists -> {
            if (playlists == null || playlists.isEmpty()) {
                Toast.makeText(this, "No playlists available", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] playlistNames = new String[playlists.size()];
            for (int i = 0; i < playlists.size(); i++) {
                playlistNames[i] = playlists.get(i).getName();
            }

            new AlertDialog.Builder(this)
                    .setTitle("Add to Playlist")
                    .setItems(playlistNames, (dialog, which) -> {
                        Playlist selected = playlists.get(which);
                        viewModel.addSongToPlaylist(selected.getId(), song.getId());
                        Toast.makeText(this, "Added to " + selected.getName(),
                                Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void removeFromPlaylist(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Remove from Playlist")
                .setMessage("Remove \"" + song.getTitle() + "\" from this playlist?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    viewModel.removeSongFromPlaylist(playlistId, song.getId());
                    Toast.makeText(this, "Removed from playlist", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Restore the item in RecyclerView
                    adapter.notifyDataSetChanged();
                })
                .show();
    }

    private void shareSong(Song song) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse("file://" + song.getPath()));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share song"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_playlist_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_rename_playlist) {
            showRenameDialog();
            return true;
        } else if (id == R.id.action_delete_playlist) {
            showDeleteConfirmation();
            return true;
        } else if (id == R.id.action_clear_playlist) {
            showClearConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showRenameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rename_playlist, null);
        TextInputEditText editText = dialogView.findViewById(R.id.editPlaylistName);
        editText.setText(currentPlaylist.getName());
        editText.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("Rename Playlist")
                .setView(dialogView)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        viewModel.renamePlaylist(playlistId, newName);
                        collapsingToolbar.setTitle(newName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete \"" + currentPlaylist.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deletePlaylist(currentPlaylist);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Playlist")
                .setMessage("Remove all songs from this playlist?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    viewModel.clearPlaylist(playlistId);
                    Toast.makeText(this, "Playlist cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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