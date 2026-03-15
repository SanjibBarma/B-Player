package b.my.audioplayer.audio_player.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import b.my.audioplayer.R;
import b.my.audioplayer.audio_player.adapter.QueueAdapter;
import b.my.audioplayer.audio_player.model.Song;
import b.my.audioplayer.audio_player.service.MusicPlaybackService;
import b.my.audioplayer.utils.WaveView;
import b.my.audioplayer.audio_player.viewmodel.MainViewModel;
import b.my.audioplayer.audio_player.viewmodel.NowPlayingViewModel;
import b.my.audioplayer.utils.Constants;
import es.dmoral.toasty.Toasty;

import java.io.Serializable;
import java.util.List;
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

        openedWithSlide = getIntent().getBooleanExtra(Constants.EXTRA_SLIDE_UP, false);

        if (openedWithSlide) {
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

        findViewById(R.id.btnSpeed).setOnClickListener(v -> showPlaybackSpeedDialog());

        songTitle.setSelected(true);
        artistName.setSelected(true);
    }

    private void setupClickListeners() {
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
                boolean currentStatus = currentSong.isFavorite();
                boolean newFavoriteStatus = !currentStatus;
                updateFavoriteIcon(newFavoriteStatus);
                mainViewModel.toggleFavorite(currentSong);

                Toasty.success(this, newFavoriteStatus ? "Added to Favorites" : "Removed from Favorites", Toasty.LENGTH_SHORT, true).show();
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

        findViewById(R.id.btnPlayList).setOnClickListener(v -> showQueueBottomSheet());

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
            if (song != null) {
                updateSongInfo(song);
                // Check favorite status from database when song changes
                checkAndUpdateFavoriteStatus(song);
            }
        });

        viewModel.getIsPlaying().observe(this, isPlaying -> {
            Log.d(TAG, "ViewModel isPlaying changed: " + isPlaying);
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
//            btnPlayPause.setColorFilter(ContextCompat.getColor(this, R.color.colorSurface));
//            btnPlayPause.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSecondary)));
        });

        viewModel.getIsShuffleEnabled().observe(this, isEnabled ->
                btnShuffle.setImageResource(
                        isEnabled ? R.drawable.ic_shuffle : R.drawable.ic_shuffle_of
                )
        );

        viewModel.getRepeatMode().observe(this, mode -> updateRepeatModeUI());

        viewModel.getDuration().observe(this, duration -> {
            if (duration != null && duration > 0) {
                seekBar.setMax((int) (long) duration);
                totalTime.setText(formatTime(duration));
            }
        });

        // ✅ KEY FIX: Observe favorite songs list to detect changes from anywhere
        mainViewModel.getFavoriteSongs().observe(this, favoriteSongs -> {
            Song currentSong = viewModel.getCurrentSong().getValue();
            if (currentSong != null && favoriteSongs != null) {
                boolean isFavorite = isSongInFavorites(currentSong, favoriteSongs);

                // Update the song object's favorite status
                if (currentSong.isFavorite() != isFavorite) {
                    currentSong.setFavorite(isFavorite);
                    updateFavoriteIcon(isFavorite);

                    // Show toast only if activity is visible and user initiated from fragment
                    Log.d(TAG, "Favorite status updated from observer: " + isFavorite);
                }
            }
        });

        // ✅ Also observe all songs for changes
        mainViewModel.getAllSongs().observe(this, songs -> {
            Song currentSong = viewModel.getCurrentSong().getValue();
            if (currentSong != null && songs != null) {
                for (Song song : songs) {
                    if (song.getId() == currentSong.getId()) {
                        if (currentSong.isFavorite() != song.isFavorite()) {
                            currentSong.setFavorite(song.isFavorite());
                            updateFavoriteIcon(song.isFavorite());
                        }
                        break;
                    }
                }
            }
        });
    }

    // ✅ Helper method to check if song is in favorites list
    private boolean isSongInFavorites(Song song, List<Song> favoriteSongs) {
        if (song == null || favoriteSongs == null) return false;

        for (Song favSong : favoriteSongs) {
            if (favSong.getId() == song.getId()) {
                return true;
            }
        }
        return false;
    }

    // ✅ Check favorite status from MainViewModel
    private void checkAndUpdateFavoriteStatus(Song song) {
        if (song == null) return;

        List<Song> favoriteSongs = mainViewModel.getFavoriteSongs().getValue();
        if (favoriteSongs != null) {
            boolean isFavorite = isSongInFavorites(song, favoriteSongs);
            song.setFavorite(isFavorite);
            updateFavoriteIcon(isFavorite);
        }
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

            // ✅ Check favorite status when updating UI
            checkAndUpdateFavoriteStatus(currentSong);
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
                btnRepeat.setImageResource(R.drawable.ic_repeat_off);
                break;
            case Player.REPEAT_MODE_ALL:
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                break;
            case Player.REPEAT_MODE_ONE:
                btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                break;
        }
    }

    private void updateShuffleUI() {
        if (!isBound || musicService == null) return;

        boolean isShuffleEnabled = musicService.getMusicPlayer().isShuffleEnabled();

        btnShuffle.setImageResource(
                isShuffleEnabled ? R.drawable.ic_shuffle : R.drawable.ic_shuffle_of
        );
    }

    private void updateSongInfo(Song song) {
        songTitle.setText(song.getTitle());
        artistName.setText(song.getArtist());

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
//        btnFavorite.setColorFilter(isFavorite
//                ? getColor(R.color.colorFavorite)
//                : getColor(R.color.colorSecondary));
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

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
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
    protected void onResume() {
        super.onResume();
        // ✅ Refresh favorite status when returning to activity
        Song currentSong = viewModel.getCurrentSong().getValue();
        if (currentSong != null) {
            checkAndUpdateFavoriteStatus(currentSong);
        }
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

    private void setupSwipeAndDrag(RecyclerView recyclerView, QueueAdapter adapter, TextView tvQueueCount) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {

            private int dragFromPosition = -1;
            private int dragToPosition = -1;

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                if (dragFromPosition == -1) {
                    dragFromPosition = fromPos;
                }
                dragToPosition = toPos;

                adapter.moveItem(fromPos, toPos);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // ✅ Position validation
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                if (!isBound || musicService == null || musicService.getMusicPlayer() == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                int currentIdx = musicService.getCurrentIndex();

                if (position == currentIdx) {
                    Toasty.warning(NowPlayingActivity.this, "Cannot remove currently playing song", Toasty.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(position);
                    return;
                }

                // ✅ Remove from queue
                musicService.getMusicPlayer().removeFromQueue(position);

                // ✅ Update UI
                List<Song> updatedQueue = musicService.getCurrentPlaylist();
                int newCurrentIndex = musicService.getCurrentIndex();

                if (updatedQueue != null) {
                    adapter.setQueue(updatedQueue, newCurrentIndex);
                    adapter.setPlayingState(newCurrentIndex, musicService.isPlaying());
                    tvQueueCount.setText(updatedQueue.size() + " songs");
                }

                Toasty.success(NowPlayingActivity.this, "Removed from queue", Toasty.LENGTH_SHORT).show();
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();

                if (position == RecyclerView.NO_POSITION) {
                    return 0;
                }

                int currentIdx = isBound && musicService != null ? musicService.getCurrentIndex() : -1;

                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = (position == currentIdx) ? 0 : ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setElevation(0f);
                viewHolder.itemView.setTranslationX(0f);

                if (dragFromPosition != -1 && dragToPosition != -1 && dragFromPosition != dragToPosition) {
                    if (isBound && musicService != null && musicService.getMusicPlayer() != null) {
                        musicService.getMusicPlayer().moveQueueItem(dragFromPosition, dragToPosition);
                        int newCurrentIndex = musicService.getCurrentIndex();
                        adapter.updateCurrentIndex(newCurrentIndex);
                    }
                }

                dragFromPosition = -1;
                dragToPosition = -1;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setElevation(16f);
                    viewHolder.itemView.setAlpha(0.85f);
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // Red background
                    android.graphics.Paint paint = new android.graphics.Paint();
                    paint.setColor(0xFFE53935);

                    if (dX > 0) {
                        c.drawRect(itemView.getLeft(), itemView.getTop(),
                                itemView.getLeft() + dX, itemView.getBottom(), paint);
                    } else if (dX < 0) {
                        c.drawRect(itemView.getRight() + dX, itemView.getTop(),
                                itemView.getRight(), itemView.getBottom(), paint);
                    }

                    // Fade effect
                    float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth();
                    itemView.setAlpha(Math.max(alpha, 0.2f));
                    itemView.setTranslationX(dX);

                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.4f; // ✅ 40% swipe করলেই delete হবে
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 0.5f; // ✅ কম velocity তেও কাজ করবে
            }

            @Override
            public float getSwipeVelocityThreshold(float defaultValue) {
                return defaultValue * 2f;
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true;
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void removeFromQueue(int position, QueueAdapter adapter, TextView tvQueueCount) {
        if (!isBound || musicService == null || musicService.getMusicPlayer() == null) {
            return;
        }

        int currentIdx = musicService.getCurrentIndex();

        if (position == currentIdx) {
            Toasty.warning(this, "Cannot remove currently playing song", Toasty.LENGTH_SHORT).show();
            List<Song> queue = musicService.getCurrentPlaylist();
            adapter.setQueue(queue, currentIdx);
            adapter.setPlayingState(currentIdx, musicService.isPlaying());
            return;
        }

        musicService.getMusicPlayer().removeFromQueue(position);

        List<Song> updatedQueue = musicService.getCurrentPlaylist();
        int newCurrentIndex = musicService.getCurrentIndex();
        adapter.setQueue(updatedQueue, newCurrentIndex);
        adapter.setPlayingState(newCurrentIndex, musicService.isPlaying());

        tvQueueCount.setText(updatedQueue.size() + " songs");
        Toasty.success(this, "Removed from queue", Toasty.LENGTH_SHORT).show();
    }

    private void showQueueBottomSheet() {
        if (!isBound || musicService == null) {
            Toasty.warning(this, "Player not ready", Toasty.LENGTH_SHORT).show();
            return;
        }

        List<Song> queue = musicService.getCurrentPlaylist();
        int currentIndex = musicService.getCurrentIndex();

        if (queue == null || queue.isEmpty()) {
            Toasty.info(this, "Queue is empty", Toasty.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_queue, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // ✅ FIX: BottomSheet Behavior setup
        BottomSheetBehavior<?> behavior = bottomSheetDialog.getBehavior();
        behavior.setDraggable(true);  // ✅ Changed to true
        behavior.setSkipCollapsed(true);  // ✅ Skip collapsed state
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);  // ✅ Start expanded
        behavior.setHideable(true);  // ✅ Allow hiding

        View parent = (View) bottomSheetView.getParent();
        parent.getLayoutParams().height = (int) (getResources().getDisplayMetrics().heightPixels * 0.7);

        TextView tvQueueTitle = bottomSheetView.findViewById(R.id.tvQueueTitle);
        TextView tvQueueCount = bottomSheetView.findViewById(R.id.tvQueueCount);
        RecyclerView recyclerViewQueue = bottomSheetView.findViewById(R.id.recyclerViewQueue);

        tvQueueTitle.setText("Playing Queue");
        tvQueueCount.setText(queue.size() + " songs");

        QueueAdapter queueAdapter = new QueueAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewQueue.setLayoutManager(layoutManager);
        recyclerViewQueue.setAdapter(queueAdapter);

        // ✅ FIX: RecyclerView touch handling
        recyclerViewQueue.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int action = e.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    // Disable BottomSheet drag when touching RecyclerView
                    rv.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}



            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        boolean isCurrentlyPlaying = musicService.isPlaying();
        queueAdapter.setQueue(queue, currentIndex);
        queueAdapter.setPlayingState(currentIndex, isCurrentlyPlaying);

        // Setup swipe and drag
        setupSwipeAndDrag(recyclerViewQueue, queueAdapter, tvQueueCount);

        if (currentIndex >= 0 && currentIndex < queue.size()) {
            recyclerViewQueue.scrollToPosition(currentIndex);
        }

        queueAdapter.setOnQueueItemClickListener(new QueueAdapter.OnQueueItemClickListener() {
            @Override
            public void onSongClick(int position) {
                if (isBound && musicService != null) {
                    int currentIdx = musicService.getCurrentIndex();

                    if (position == currentIdx) {
                        return;
                    }

                    musicService.playSongAt(position);
                    queueAdapter.setPlayingState(position, true);
                }
            }

            @Override
            public void onRemoveClick(int position) {
                removeFromQueue(position, queueAdapter, tvQueueCount);
            }
        });

        // ✅ FIX: Smooth dismiss on outside touch
        bottomSheetDialog.setCanceledOnTouchOutside(true);

        bottomSheetDialog.show();
    }

    private void showPlaybackSpeedDialog() {
        if (!isBound || musicService == null) {
            Toasty.warning(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playback_speed, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Views
        TextView tvCurrentSpeed = dialogView.findViewById(R.id.tvCurrentSpeed);
        SeekBar seekBarSpeed = dialogView.findViewById(R.id.seekBarSpeed);
        ConstraintLayout btnSpeed05 = dialogView.findViewById(R.id.btnSpeed05);
        ConstraintLayout btnSpeed075 = dialogView.findViewById(R.id.btnSpeed075);
        ConstraintLayout btnSpeed10 = dialogView.findViewById(R.id.btnSpeed10);
        ConstraintLayout btnSpeed125 = dialogView.findViewById(R.id.btnSpeed125);
        ConstraintLayout btnSpeed15 = dialogView.findViewById(R.id.btnSpeed15);
        ConstraintLayout btnSpeed20 = dialogView.findViewById(R.id.btnSpeed20);
        ConstraintLayout btnResetSpeed = dialogView.findViewById(R.id.btnResetSpeed);
        ConstraintLayout btnApplySpeed = dialogView.findViewById(R.id.btnApplySpeed);
        ImageButton btnClose = dialogView.findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Get current speed
        float currentSpeed = musicService.getMusicPlayer().getPlaybackSpeed();

        // SeekBar: 0.5x to 2.0x (progress 0-30 → 0.5-2.0)
        // Formula: speed = 0.5 + (progress * 0.05)
        int currentProgress = (int) ((currentSpeed - 0.5f) / 0.05f);
        seekBarSpeed.setProgress(currentProgress);
        tvCurrentSpeed.setText(String.format("%.2fx", currentSpeed));

        // SeekBar listener
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.5f + (progress * 0.05f);
                tvCurrentSpeed.setText(String.format("%.2fx", speed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Quick select buttons
        View.OnClickListener speedClickListener = v -> {
            float speed = 1.0f;
            if (v.getId() == R.id.btnSpeed05) speed = 0.5f;
            else if (v.getId() == R.id.btnSpeed075) speed = 0.75f;
            else if (v.getId() == R.id.btnSpeed10) speed = 1.0f;
            else if (v.getId() == R.id.btnSpeed125) speed = 1.25f;
            else if (v.getId() == R.id.btnSpeed15) speed = 1.5f;
            else if (v.getId() == R.id.btnSpeed20) speed = 2.0f;

            int progress = (int) ((speed - 0.5f) / 0.05f);
            seekBarSpeed.setProgress(progress);
            tvCurrentSpeed.setText(String.format("%.2fx", speed));
        };

        btnSpeed05.setOnClickListener(speedClickListener);
        btnSpeed075.setOnClickListener(speedClickListener);
        btnSpeed10.setOnClickListener(speedClickListener);
        btnSpeed125.setOnClickListener(speedClickListener);
        btnSpeed15.setOnClickListener(speedClickListener);
        btnSpeed20.setOnClickListener(speedClickListener);

        // Reset button
        btnResetSpeed.setOnClickListener(v -> {
            seekBarSpeed.setProgress(10); // 1.0x
            tvCurrentSpeed.setText("1.00x");
        });

        // Apply button
        btnApplySpeed.setOnClickListener(v -> {
            float speed = 0.5f + (seekBarSpeed.getProgress() * 0.05f);
            musicService.getMusicPlayer().setPlaybackSpeed(speed);
            Toasty.success(this, "Speed set to " + String.format("%.2fx", speed), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();

        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);
    }
}