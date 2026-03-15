package b.my.audioplayer.audio_player.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import b.my.audioplayer.R;
import b.my.audioplayer.audio_player.adapter.PlaylistAdapter;
import b.my.audioplayer.audio_player.adapter.SongAdapter;
import b.my.audioplayer.audio_player.model.Playlist;
import b.my.audioplayer.audio_player.model.Song;
import b.my.audioplayer.audio_player.service.MusicPlaybackService;
import b.my.audioplayer.audio_player.viewmodel.MainViewModel;
import b.my.audioplayer.audio_player.viewmodel.PlaylistViewModel;
import b.my.audioplayer.utils.Constants;
import es.dmoral.toasty.Toasty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private PlaylistViewModel viewModel;
    private MainViewModel mainViewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView playlistCoverArt;
    private ConstraintLayout btnPlayAll;
    private ConstraintLayout btnShuffleAll;
    private FloatingActionButton fabAddSongs;
    private MaterialToolbar toolbar;
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
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

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

        // Set colors
        setupToolbarColors();

        toolbar.setNavigationOnClickListener(v -> finish());

        btnPlayAll.setOnClickListener(v -> playAll(false));
        btnShuffleAll.setOnClickListener(v -> playAll(true));
        fabAddSongs.setOnClickListener(v -> showAddSongsDialog());
    }

    private void setupToolbarColors() {
        int color = ContextCompat.getColor(this, R.color.colorSecondary);

        // CollapsingToolbar title colors
        collapsingToolbar.setExpandedTitleColor(color);
        collapsingToolbar.setCollapsedTitleTextColor(color);

        // Navigation icon (back arrow) color
        Drawable navIcon = toolbar.getNavigationIcon();
        if (navIcon != null) {
            navIcon = DrawableCompat.wrap(navIcon).mutate();
//            DrawableCompat.setTint(navIcon, color);
            toolbar.setNavigationIcon(navIcon);
        }

        // Overflow menu icon color
        Drawable overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_more_vert);
        if (overflowIcon != null) {
            overflowIcon = DrawableCompat.wrap(overflowIcon).mutate();
            DrawableCompat.setTint(overflowIcon, color);
            toolbar.setOverflowIcon(overflowIcon);
        }
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
                    Toasty.info(PlaylistDetailActivity.this, "Will play next", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAddToPlaylist(Song song) {
                showAddToPlaylistDialog(song);
            }

            @Override
            public void onToggleFavorite(Song song) {
                mainViewModel.toggleFavorite(song);
                Toasty.info(PlaylistDetailActivity.this,
                        song.isFavorite() ? "Added to Favorites" : "Removed from Favorites",
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
            Toasty.info(this, "Playlist is empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddSongsDialog() {
        Intent intent = new Intent(this, SongPickerActivity.class);
        intent.putExtra(Constants.EXTRA_PLAYLIST_ID, playlistId);
        startActivity(intent);
    }

    private void showAddToPlaylistDialog(Song song) {
        viewModel.getAllPlaylists().observe(this, playlists -> {
            if (playlists == null || playlists.isEmpty()) {
                Toasty.info(this, "No playlists available", Toast.LENGTH_SHORT).show();
                return;
            }

            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist_picker, null);
            RecyclerView rv = dialogView.findViewById(R.id.recyclerViewPlaylists);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            PlaylistAdapter playlistAdapter = new PlaylistAdapter(this);
            playlistAdapter.setPlaylists(playlists);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(playlistAdapter);

            playlistAdapter.setOnPlaylistClickListener(selected -> {
                viewModel.addSongToPlaylist(selected.getId(), song.getId());
                Toasty.success(this, "Added to " + selected.getName(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
        });
    }

    private void removeFromPlaylist(Song song) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvTitle.setText("Remove from Playlist");
        tvMessage.setText("Remove \"" + song.getTitle() + "\" from this playlist?");
        btnConfirm.setText("Remove");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirm.setOnClickListener(v -> {
            viewModel.removeSongFromPlaylist(playlistId, song.getId());
            Toasty.success(this, "Removed from playlist", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            adapter.notifyDataSetChanged();
            dialog.dismiss();
        });
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int color = ContextCompat.getColor(this, R.color.colorSecondary);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Drawable icon = item.getIcon();
            if (icon != null) {
                icon = DrawableCompat.wrap(icon).mutate();
                DrawableCompat.setTint(icon, color);
                item.setIcon(icon);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void showRenameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rename_playlist, null);
        TextInputEditText editText = dialogView.findViewById(R.id.editPlaylistName);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnRename = dialogView.findViewById(R.id.btnRename);

        editText.setText(currentPlaylist.getName());
        editText.selectAll();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnRename.setOnClickListener(v -> {
            String newName = editText.getText().toString().trim();
            if (!newName.isEmpty()) {
                viewModel.renamePlaylist(playlistId, newName);
                collapsingToolbar.setTitle(newName);
                Toasty.success(this, "Playlist renamed", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showDeleteConfirmation() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvTitle.setText("Delete Playlist");
        tvMessage.setText("Are you sure you want to delete \"" + currentPlaylist.getName() + "\"?");
        btnConfirm.setText("Delete");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirm.setOnClickListener(v -> {
            viewModel.deletePlaylist(currentPlaylist);
            Toasty.success(this, "Playlist deleted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            finish();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void showClearConfirmation() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvTitle.setText("Clear Playlist");
        tvMessage.setText("Remove all songs from this playlist?");
        btnConfirm.setText("Clear");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConfirm.setOnClickListener(v -> {
            viewModel.clearPlaylist(playlistId);
            Toasty.success(this, "Playlist cleared", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
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