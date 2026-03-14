package b.my.audioplayer.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import b.my.audioplayer.R;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.service.MusicPlaybackService;
import b.my.audioplayer.utils.WaveView;
import b.my.audioplayer.viewmodel.MainViewModel;
import b.my.audioplayer.viewmodel.NowPlayingViewModel;
import b.my.audioplayer.utils.Constants;
import es.dmoral.toasty.Toasty;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class NowPlayingActivity extends AppCompatActivity {

    private static final String TAG = "NowPlayingActivity";

    private NowPlayingViewModel viewModel;
    private MainViewModel mainViewModel;
    private MusicPlaybackService musicService;
    private boolean isBound = false;

    private ImageView albumArt;
    private TextView songTitle;
    private TextView artistName;
    private TextView currentTime;
    private TextView totalTime;
    private TextView tvPlayingTitle;
    private SeekBar seekBar;
    private ImageButton btnPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private ImageButton btnFavorite;
    private ImageButton btnBack;
    private ImageButton btnLyrics;
    private WaveView waveView;
    private boolean openedWithSlide = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarRunnable;
    private boolean isUserSeeking = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            MusicPlaybackService.MusicBinder binder = (MusicPlaybackService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            setupPlayerListeners();
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            isBound = false;
            stopSeekBarUpdate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if should use slide animation
        openedWithSlide = getIntent().getBooleanExtra(Constants.EXTRA_SLIDE_UP, false);

        if (openedWithSlide) {
            // Apply slide up enter animation only when opened from mini player
            overridePendingTransition(R.anim.slide_up_enter, R.anim.no_animation);
        }

        setContentView(R.layout.activity_now_playing);

        viewModel = new ViewModelProvider(this).get(NowPlayingViewModel.class);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModel();
        bindService();
        setupBackPressHandler();
    }

    private void initViews() {
        albumArt = findViewById(R.id.nowPlayingAlbumArt);
        songTitle = findViewById(R.id.nowPlayingTitle);
        artistName = findViewById(R.id.nowPlayingArtist);
        seekBar = findViewById(R.id.seekBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnBack = findViewById(R.id.btnBack);
        btnLyrics = findViewById(R.id.btnLyrics);
        tvPlayingTitle = findViewById(R.id.tvPlayingTitle);
        waveView = findViewById(R.id.waveView);

        songTitle.setSelected(true);
        artistName.setSelected(true);
    }

    private void setupClickListeners() {
        // FIX: Use custom finish with animation
        btnBack.setOnClickListener(v -> finishWithAnimation());

        btnPlayPause.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.togglePlayback();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playPrevious();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.playNext();
            }
        });

        btnShuffle.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.getMusicPlayer().toggleShuffle();
                viewModel.toggleShuffle();
                updateShuffleUI();
                Toasty.info(this,
                        musicService.getMusicPlayer().isShuffleEnabled() ? "Shuffle On" : "Shuffle Off",
                        Toasty.LENGTH_SHORT, true).show();
            }
        });

        btnRepeat.setOnClickListener(v -> {
            if (isBound && musicService != null) {
                musicService.getMusicPlayer().cycleRepeatMode();
                viewModel.cycleRepeatMode();
                updateRepeatModeUI();
                int mode = musicService.getMusicPlayer().getRepeatMode();
                String msg = mode == Player.REPEAT_MODE_OFF ? "Repeat Off"
                        : mode == Player.REPEAT_MODE_ALL ? "Repeat All"
                        : "Repeat One";
                Toasty.info(this, msg, Toasty.LENGTH_SHORT, true).show();
            }
        });

        btnFavorite.setOnClickListener(v -> {
            Song currentSong = viewModel.getCurrentSong().getValue();
            if (currentSong != null) {
                mainViewModel.toggleFavorite(currentSong);
                updateFavoriteIcon(currentSong.isFavorite());
                Toasty.success(this,
                        currentSong.isFavorite() ? "Added to Favorites" : "Removed from Favorites",
                        Toasty.LENGTH_SHORT, true).show();
            }
        });

        btnLyrics.setOnClickListener(v -> {
            Song currentSong = viewModel.getCurrentSong().getValue();
            if (currentSong != null) {
                Intent intent = new Intent(this, LyricsActivity.class);
                intent.putExtra(Constants.EXTRA_SONG, (Serializable) currentSong);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnEqualizer).setOnClickListener(v ->
                startActivity(new Intent(this, EqualizerActivity.class)));

        findViewById(R.id.btnSleepTimer).setOnClickListener(v -> showSleepTimerDialog());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBound && musicService != null) {
                    musicService.seekTo((long) seekBar.getProgress());
                }
                isUserSeeking = false;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Custom finish with slide down animation
    // -------------------------------------------------------------------------

    private void finishWithAnimation() {
        finish();
        overridePendingTransition(R.anim.no_animation, R.anim.slide_down_exit);
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithAnimation();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getCurrentSong().observe(this, song -> {
            if (song != null) updateSongInfo(song);
        });

        viewModel.getIsPlaying().observe(this, isPlaying -> {
            Log.d(TAG, "ViewModel isPlaying changed: " + isPlaying);
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            btnPlayPause.setColorFilter(ContextCompat.getColor(this, R.color.colorSurface));
            btnPlayPause.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSecondary)));
        });

        viewModel.getIsShuffleEnabled().observe(this, isEnabled ->
                btnShuffle.setColorFilter(isEnabled
                        ? getColor(R.color.colorSurface)
                        : getColor(R.color.colorSecondary)));

        viewModel.getRepeatMode().observe(this, mode -> updateRepeatModeUI());

        viewModel.getDuration().observe(this, duration -> {
            if (duration != null && duration > 0) {
                seekBar.setMax((int) (long) duration);
                totalTime.setText(formatTime(duration));
            }
        });
    }

    private void setupPlayerListeners() {
        if (!isBound || musicService == null) return;

        musicService.getMusicPlayer().getExoPlayer().addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long duration = musicService.getMusicPlayer().getExoPlayer().getDuration();
                    if (duration > 0) {
                        viewModel.setDuration(duration);
                    }
                }
                updateUI();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "Player onIsPlayingChanged: " + isPlaying);
                viewModel.setIsPlaying(isPlaying);

                runOnUiThread(() -> {
                    if (isPlaying) {
                        startSeekBarUpdate();
                        waveView.startAnimation();
                        waveView.setVisibility(View.VISIBLE);
                    } else {
                        stopSeekBarUpdate();
                        waveView.stopAnimation();
                        waveView.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onMediaItemTransition(androidx.media3.common.MediaItem mediaItem, int reason) {
                Log.d(TAG, "onMediaItemTransition");
                stopSeekBarUpdate();
                runOnUiThread(() -> {
                    seekBar.setProgress(0);
                    currentTime.setText(formatTime(0));
                    totalTime.setText(formatTime(0));
                    updateUI();
                });
            }
        });
    }

    private void updateUI() {
        if (!isBound || musicService == null) return;

        Song currentSong = musicService.getCurrentSong();

        if (currentSong != null) {
            viewModel.setCurrentSong(currentSong);

            long duration = musicService.getMusicPlayer().getExoPlayer().getDuration();
            if (duration > 0) {
                viewModel.setDuration(duration);
            }
        }

        boolean isPlaying = musicService.isPlaying();
        viewModel.setIsPlaying(isPlaying);

        if (isPlaying) {
            startSeekBarUpdate();
            waveView.startAnimation();
            waveView.setVisibility(View.VISIBLE);
        } else {
            stopSeekBarUpdate();
            waveView.stopAnimation();
            waveView.setVisibility(View.GONE);
        }

        long position = musicService.getMusicPlayer().getExoPlayer().getCurrentPosition();
        if (!isUserSeeking && position >= 0) {
            seekBar.setProgress((int) position);
            currentTime.setText(formatTime(position));
        }

        updateRepeatModeUI();
    }

    private void updateRepeatModeUI() {
        if (!isBound || musicService == null) return;

        int mode = musicService.getMusicPlayer().getRepeatMode();
        switch (mode) {
            case Player.REPEAT_MODE_OFF:
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(getColor(R.color.colorSurface));
                break;
            case Player.REPEAT_MODE_ALL:
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(getColor(R.color.colorSecondary));
                break;
            case Player.REPEAT_MODE_ONE:
                btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnRepeat.setColorFilter(getColor(R.color.colorSecondary));
                break;
        }
    }

    private void updateShuffleUI() {
        if (!isBound || musicService == null) return;
        boolean isShuffleEnabled = musicService.getMusicPlayer().isShuffleEnabled();
        btnShuffle.setColorFilter(isShuffleEnabled
                ? getColor(R.color.colorSurface)
                : getColor(R.color.colorSecondary));
    }

    private void updateSongInfo(Song song) {
        songTitle.setText(song.getTitle());
        artistName.setText(song.getArtist());

        // Re-enable marquee
        songTitle.setSelected(true);
        artistName.setSelected(true);

        if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
            Glide.with(this)
                    .load(song.getAlbumArt())
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(albumArt);
        } else {
            albumArt.setImageResource(R.drawable.ic_music_note);
        }

        updateFavoriteIcon(song.isFavorite());
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        btnFavorite.setImageResource(isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        btnFavorite.setColorFilter(isFavorite
                ? getColor(R.color.colorFavorite)
                : getColor(R.color.colorSecondary));
    }

    private void startSeekBarUpdate() {
        stopSeekBarUpdate();
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (isBound && musicService != null && !isUserSeeking) {
                    long position = musicService.getMusicPlayer().getExoPlayer().getCurrentPosition();
                    seekBar.setProgress((int) position);
                    currentTime.setText(formatTime(position));
                    viewModel.setCurrentPosition(position);
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateSeekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
            updateSeekBarRunnable = null;
        }
    }

    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
    }

    private void showSleepTimerDialog() {
        String[] options = {"10 minutes", "20 minutes", "30 minutes", "60 minutes", "Cancel timer"};

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_options_list, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        RecyclerView rvOptions = dialogView.findViewById(R.id.recyclerViewOptions);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        tvTitle.setText("Sleep Timer");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialog.show();

        // Optional: Rounded corners and proper width
        if (dialog.getWindow() != null) {
//            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.9), // 90% of screen width
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        rvOptions.setLayoutManager(new LinearLayoutManager(this));
        rvOptions.setAdapter(new RecyclerView.Adapter<OptionViewHolder>() {
            @NonNull
            @Override
            public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_option, parent, false);
                return new OptionViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
                holder.tvOption.setText(options[position]);
                holder.itemView.setOnClickListener(v -> {
                    if (position == 4) {
                        Toasty.info(NowPlayingActivity.this, "Sleep timer cancelled", Toast.LENGTH_SHORT).show();
                    } else {
                        int[] mins = {10, 20, 30, 60};
                        int m = mins[position];
                        handler.postDelayed(() -> {
                            if (isBound) musicService.pause();
                        }, m * 60 * 1000L);
                        Toasty.success(NowPlayingActivity.this, "Sleep timer set for " + m + " minutes", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return options.length;
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private static class OptionViewHolder extends RecyclerView.ViewHolder {
        TextView tvOption;
        OptionViewHolder(View view) {
            super(view);
            tvOption = view.findViewById(R.id.tvOptionName);
        }
    }

    private void setSleepTimer(int minutes) {
        handler.postDelayed(() -> {
            if (isBound && musicService != null) {
                musicService.pause();
            }
        }, minutes * 60 * 1000L);
        Toasty.info(this, "Sleep timer set for " + minutes + " minutes", Toasty.LENGTH_SHORT, true).show();
    }

    private void cancelSleepTimer() {
        Toasty.info(this, "Sleep timer cancelled", Toasty.LENGTH_SHORT, true).show();
    }

    private void bindService() {
        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSeekBarUpdate();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}