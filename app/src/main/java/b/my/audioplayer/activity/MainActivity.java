package b.my.audioplayer.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.Player;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import b.my.audioplayer.R;
import b.my.audioplayer.fragment.AlbumsFragment;
import b.my.audioplayer.fragment.FavoritesFragment;
import b.my.audioplayer.fragment.PlaylistsFragment;
import b.my.audioplayer.fragment.SongsFragment;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.utils.Constants;
import b.my.audioplayer.utils.PermissionHelper;
import b.my.audioplayer.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigation;

    // Mini Player Views
    private View miniPlayerContainer;
    private ImageView miniPlayerAlbumArt;
    private TextView miniPlayerTitle, tvAppBarTitle;
    private TextView miniPlayerArtist;
    private ImageButton miniPlayerBtnPlayPause;
    private ImageButton miniPlayerBtnNext;
    private ImageButton miniPlayerBtnPrevious;
    private ProgressBar miniPlayerProgress;

    private MusicPlaybackService musicService;
    private boolean isBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.MusicBinder binder = (MusicPlaybackService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            attachExoPlayerListener();
            updateMiniPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            stopProgressUpdate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        setupToolbar();
        setupBottomNavigation();
        setupMiniPlayer();
        checkPermissions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        miniPlayerContainer = findViewById(R.id.miniPlayerContainer);
        miniPlayerAlbumArt = findViewById(R.id.miniPlayerAlbumArt);
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle);
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist);
        miniPlayerBtnPlayPause = findViewById(R.id.miniPlayerBtnPlayPause);
        miniPlayerBtnNext = findViewById(R.id.miniPlayerBtnNext);
        miniPlayerBtnPrevious = findViewById(R.id.miniPlayerBtnPrevious);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);
        tvAppBarTitle = findViewById(R.id.tvAppBarTitle);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("B Player");
        miniPlayerTitle.setSelected(true);
    }

    // Cached fragment instances — never re-created, just shown/hidden.
    // This keeps SongsFragment alive across tab switches so its service
    // binding, adapter state, and scroll position are always current.
    private SongsFragment songsFragment;
    private AlbumsFragment albumsFragment;
    private PlaylistsFragment playlistsFragment;
    private FavoritesFragment favoritesFragment;
    private Fragment activeFragment;

    private void setupBottomNavigation() {
        songsFragment = new SongsFragment();
        albumsFragment = new AlbumsFragment();
        playlistsFragment = new PlaylistsFragment();
        favoritesFragment = new FavoritesFragment();
        activeFragment = songsFragment;

        // Add all fragments once; only Songs is visible initially
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, favoritesFragment).hide(favoritesFragment)
                .add(R.id.fragmentContainer, playlistsFragment).hide(playlistsFragment)
                .add(R.id.fragmentContainer, albumsFragment).hide(albumsFragment)
                .add(R.id.fragmentContainer, songsFragment)
                .commit();

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment target = null;
            int id = item.getItemId();
            if (id == R.id.nav_songs) target = songsFragment;
            else if (id == R.id.nav_albums) target = albumsFragment;
            else if (id == R.id.nav_playlists) target = playlistsFragment;
            else if (id == R.id.nav_favorites) target = favoritesFragment;

            if (target != null && target != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(target)
                        .commit();
                activeFragment = target;

                // Switching back to Songs — scroll to the currently playing song
                if (target == songsFragment) {
                    songsFragment.scrollToPlayingIfNeeded();
                }
            }
            return target != null;
        });

        bottomNavigation.setSelectedItemId(R.id.nav_songs);
    }

    private void setupMiniPlayer() {
        miniPlayerContainer.setOnClickListener(v -> {
            Intent intent = new Intent(this, NowPlayingActivity.class);
            intent.putExtra(Constants.EXTRA_SLIDE_UP, true);  // Slide up animation
            startActivity(intent);
        });

        miniPlayerBtnPlayPause.setOnClickListener(v -> {
            if (isBound) {
                if (musicService.getMusicPlayer().isPlaying()) musicService.pause();
                else musicService.play();
            }
        });

        miniPlayerBtnNext.setOnClickListener(v -> {
            if (isBound) musicService.playNext();
        });

        miniPlayerBtnPrevious.setOnClickListener(v -> {
            if (isBound) musicService.playPrevious();
        });
    }

    // -------------------------------------------------------------------------
    // ExoPlayer listener — attached once, never displaced by setCallback() calls
    // -------------------------------------------------------------------------

    private void attachExoPlayerListener() {
        musicService.getMusicPlayer().getExoPlayer().addListener(new Player.Listener() {

            // play / pause toggled
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                runOnUiThread(() -> {
                    updatePlayPauseButton(isPlaying);
                    if (isPlaying && progressRunnable == null) {
                        startProgressUpdate();
                    }
                });
            }

            // next / previous / auto-advance — song changed
            @Override
            public void onMediaItemTransition(
                    androidx.media3.common.MediaItem mediaItem, int reason) {
                runOnUiThread(() -> {
                    miniPlayerProgress.setProgress(0);
                    updateMiniPlayer();
                });
            }

            // track is buffered and ready — safe to read duration
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    runOnUiThread(() -> updateMiniPlayer());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Mini player helpers
    // -------------------------------------------------------------------------

    private void updateMiniPlayer() {
        if (!isBound) return;

        Song currentSong = musicService.getMusicPlayer().getCurrentSong();
        if (currentSong != null) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
            miniPlayerTitle.setText(currentSong.getTitle());
            miniPlayerArtist.setText(currentSong.getArtist());

            if (currentSong.getAlbumArt() != null) {
                Glide.with(this)
                        .load(currentSong.getAlbumArt())
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(miniPlayerAlbumArt);
            } else {
                miniPlayerAlbumArt.setImageResource(R.drawable.ic_music_note);
            }

            updatePlayPauseButton(musicService.getMusicPlayer().isPlaying());
            startProgressUpdate();
        } else {
            miniPlayerContainer.setVisibility(View.GONE);
            stopProgressUpdate();
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        miniPlayerBtnPlayPause.setImageResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void startProgressUpdate() {
        stopProgressUpdate();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound) {
                    long position = musicService.getMusicPlayer().getCurrentPosition();
                    long duration = musicService.getMusicPlayer().getDuration();
                    if (duration > 0) {
                        miniPlayerProgress.setProgress((int) ((position * 100) / duration));
                    }
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.post(progressRunnable);
    }

    private void stopProgressUpdate() {
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    // -------------------------------------------------------------------------
    // Permissions & service
    // -------------------------------------------------------------------------

    private void checkPermissions() {
        if (!PermissionHelper.hasPermissions(this)) PermissionHelper.requestPermissions(this);
        else {
            loadMusic();
            startMusicService();
        }
    }

    private void loadMusic() {
        viewModel.scanMediaFiles(this);
    }

    private void startMusicService() {
        Intent intent = new Intent(this, MusicPlaybackService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusic();
                startMusicService();
            } else {
                Toast.makeText(this, "Permission required to access music files",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Menus
    // -------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search songs...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {
                viewModel.setSearchQuery(q);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String q) {
                viewModel.setSearchQuery(q);
                return true;
            }
        });
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                viewModel.setSearchQuery("");
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) return true;
        if (id == R.id.sort_title) {
            viewModel.setSortOrder(Constants.SORT_BY_TITLE);
            return true;
        }
        if (id == R.id.sort_artist) {
            viewModel.setSortOrder(Constants.SORT_BY_ARTIST);
            return true;
        }
        if (id == R.id.sort_album) {
            viewModel.setSortOrder(Constants.SORT_BY_ALBUM);
            return true;
        }
        if (id == R.id.sort_date_added) {
            viewModel.setSortOrder(Constants.SORT_BY_DATE);
            return true;
        }
        if (id == R.id.sort_duration) {
            viewModel.setSortOrder(Constants.SORT_BY_DURATION);
            return true;
        }
        if (id == R.id.action_equalizer) {
            startActivity(new Intent(this, EqualizerActivity.class));
            return true;
        }
        if (id == R.id.action_sleep_timer) {
            showSleepTimerDialog();
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSleepTimerDialog() {
        String[] options = {"10 minutes", "20 minutes", "30 minutes", "60 minutes", "Cancel timer"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sleep Timer")
                .setItems(options, (dialog, which) -> {
                    if (which == 4) {
                        Toast.makeText(this, "Sleep timer cancelled", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int[] mins = {10, 20, 30, 60};
                    int m = mins[which];
                    handler.postDelayed(() -> {
                        if (isBound) musicService.pause();
                    }, m * 60 * 1000L);
                    Toast.makeText(this, "Sleep timer set for " + m + " minutes",
                            Toast.LENGTH_SHORT).show();
                }).show();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        if (isBound) updateMiniPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdate();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}