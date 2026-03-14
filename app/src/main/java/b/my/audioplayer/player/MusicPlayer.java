package b.my.audioplayer.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import b.my.audioplayer.database.AppDatabase;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.PreferenceHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicPlayer {

    private ExoPlayer exoPlayer;
    private Context context;
    private List<Song> originalPlaylist;
    private List<Song> currentPlaylist;
    private int currentIndex = 0;
    private boolean isShuffleEnabled = false;
    private int repeatMode = Player.REPEAT_MODE_OFF;
    private PreferenceHelper preferenceHelper;
    private PlayerCallback callback;
    private Handler handler;
    private Runnable positionUpdateRunnable;
    private boolean isPositionUpdateRunning = false;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface PlayerCallback {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(Song song);
        void onPositionChanged(long position, long duration);
        void onPlaybackError(String message);
    }

    public MusicPlayer(Context context) {
        this.context = context;
        this.preferenceHelper = new PreferenceHelper(context);
        this.originalPlaylist = new ArrayList<>();
        this.currentPlaylist = new ArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());

        initializePlayer();
        loadSavedState();
    }

    private void initializePlayer() {
        exoPlayer = new ExoPlayer.Builder(context).build();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    handlePlaybackEnded();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (callback != null) {
                    callback.onPlaybackStateChanged(isPlaying);
                }
                if (isPlaying) {
                    startPositionUpdates();
                } else {
                    stopPositionUpdates();
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                int newIndex = exoPlayer.getCurrentMediaItemIndex();
                if (newIndex >= 0 && newIndex < currentPlaylist.size()) {
                    currentIndex = newIndex;
                    if (callback != null) {
                        callback.onSongChanged(currentPlaylist.get(currentIndex));
                    }
                }
                saveCurrentState();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (callback != null) {
                    callback.onPlaybackError(error.getMessage());
                }
            }
        });
    }

    private void loadSavedState() {
        isShuffleEnabled = preferenceHelper.isShuffleEnabled();
        repeatMode = preferenceHelper.getRepeatMode();
        exoPlayer.setRepeatMode(repeatMode);

        long lastSongId = preferenceHelper.getLastPlayedSongId();
        executorService.execute(() -> {
            List<Song> allSongs = AppDatabase.getInstance(context).songDao().getAllSongsSync();
            if (allSongs != null && !allSongs.isEmpty()) {
                handler.post(() -> {
                    // Use internal method that respects saved shuffle state
                    setPlaylistInternal(allSongs, true);

                    if (lastSongId != -1) {
                        for (int i = 0; i < currentPlaylist.size(); i++) {
                            if (currentPlaylist.get(i).getId() == lastSongId) {
                                currentIndex = i;
                                break;
                            }
                        }
                    }
                    exoPlayer.seekTo(currentIndex, preferenceHelper.getLastPosition());
                    if (callback != null && currentIndex < currentPlaylist.size()) {
                        callback.onSongChanged(currentPlaylist.get(currentIndex));
                    }
                });
            }
        });
    }

    private void saveCurrentState() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            preferenceHelper.setLastPlayedSongId(currentPlaylist.get(currentIndex).getId());
        }
        preferenceHelper.setLastPosition(exoPlayer.getCurrentPosition());
        preferenceHelper.setShuffleEnabled(isShuffleEnabled);
        preferenceHelper.setRepeatMode(repeatMode);
    }

    public void setCallback(PlayerCallback callback) {
        this.callback = callback;
    }

    /**
     * Set playlist - plays in order (ignores shuffle state for new playlist)
     */
    public void setPlaylist(List<Song> songs) {
        setPlaylist(songs, 0);
    }

    /**
     * Set playlist with start index - plays in order from that index
     * Shuffle is NOT applied to new playlist automatically
     */
    public void setPlaylist(List<Song> songs, int startIndex) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPlaylist = new ArrayList<>(songs);

        // Ensure startIndex is valid
        if (startIndex < 0 || startIndex >= currentPlaylist.size()) {
            this.currentIndex = 0;
        } else {
            this.currentIndex = startIndex;
        }

        // DO NOT shuffle when setting new playlist
        // User must explicitly enable shuffle

        preparePlaylist(0);
    }

    /**
     * Internal method - respects shuffle state (used for restoring state)
     */
    private void setPlaylistInternal(List<Song> songs, boolean applyShuffle) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPlaylist = new ArrayList<>(songs);
        this.currentIndex = 0;

        if (applyShuffle && isShuffleEnabled) {
            shufflePlaylist();
        }

        preparePlaylist(0);
    }

    private void preparePlaylist(long startPositionMs) {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : currentPlaylist) {
            MediaItem mediaItem = MediaItem.fromUri(song.getPath());
            mediaItems.add(mediaItem);
        }

        exoPlayer.setMediaItems(mediaItems, currentIndex, startPositionMs);
        exoPlayer.prepare();
    }

    private void shufflePlaylist() {
        Song currentSong = null;
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            currentSong = currentPlaylist.get(currentIndex);
        }

        Collections.shuffle(currentPlaylist);

        if (currentSong != null) {
            currentPlaylist.remove(currentSong);
            currentPlaylist.add(0, currentSong);
            currentIndex = 0;
        }
    }

    public void play() {
        if (exoPlayer == null) {
            return;
        }

        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            if (callback != null) {
                callback.onPlaybackError("Playlist is empty");
            }
            return;
        }

        if (currentIndex < 0 || currentIndex >= currentPlaylist.size()) {
            currentIndex = 0;
        }

        exoPlayer.play();
    }

    public void pause() {
        exoPlayer.pause();
        saveCurrentState();
    }

    public void stop() {
        exoPlayer.stop();
        saveCurrentState();
    }

    public void playNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem();
            play();
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            exoPlayer.seekTo(0, 0);
            play();
        }
    }

    public void playPrevious() {
        if (exoPlayer.getCurrentPosition() > 3000) {
            exoPlayer.seekTo(0);
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem();
            play();
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            exoPlayer.seekTo(currentPlaylist.size() - 1, 0);
            play();
        }
    }

    public void playSongAt(int index) {
        if (index >= 0 && index < currentPlaylist.size()) {
            currentIndex = index;
            exoPlayer.seekTo(currentIndex, 0);
            play();
        }
    }

    public void seekTo(long position) {
        exoPlayer.seekTo(position);
    }

    public void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        long currentPos = exoPlayer.getCurrentPosition();

        Song currentSong = getCurrentSong();
        if (isShuffleEnabled) {
            shufflePlaylist();
        } else {
            currentPlaylist = new ArrayList<>(originalPlaylist);
            if (currentSong != null) {
                currentIndex = currentPlaylist.indexOf(currentSong);
                if (currentIndex < 0) currentIndex = 0;
            }
        }

        preparePlaylist(currentPos);
        preferenceHelper.setShuffleEnabled(isShuffleEnabled);
    }

    public void cycleRepeatMode() {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                repeatMode = Player.REPEAT_MODE_ALL;
                break;
            case Player.REPEAT_MODE_ALL:
                repeatMode = Player.REPEAT_MODE_ONE;
                break;
            case Player.REPEAT_MODE_ONE:
                repeatMode = Player.REPEAT_MODE_OFF;
                break;
        }
        exoPlayer.setRepeatMode(repeatMode);
        preferenceHelper.setRepeatMode(repeatMode);
    }

    public void setRepeatMode(int mode) {
        this.repeatMode = mode;
        exoPlayer.setRepeatMode(mode);
        preferenceHelper.setRepeatMode(mode);
    }

    private void handlePlaybackEnded() {
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            if (!exoPlayer.hasNextMediaItem()) {
                stop();
            }
        }
    }

    private void startPositionUpdates() {
        if (isPositionUpdateRunning) return;

        isPositionUpdateRunning = true;
        positionUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (callback != null && exoPlayer.isPlaying()) {
                    callback.onPositionChanged(
                            exoPlayer.getCurrentPosition(),
                            exoPlayer.getDuration()
                    );
                }
                if (isPositionUpdateRunning) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(positionUpdateRunnable);
    }

    private void stopPositionUpdates() {
        isPositionUpdateRunning = false;
        if (positionUpdateRunnable != null) {
            handler.removeCallbacks(positionUpdateRunnable);
        }
    }

    // Getters
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }

    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<Song> getCurrentPlaylist() {
        return currentPlaylist;
    }

    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return exoPlayer.getDuration();
    }

    public int getAudioSessionId() {
        return exoPlayer.getAudioSessionId();
    }

    public void addToQueue(Song song) {
        if (currentPlaylist == null) {
            currentPlaylist = new ArrayList<>();
        }
        if (originalPlaylist == null) {
            originalPlaylist = new ArrayList<>();
        }

        currentPlaylist.add(song);
        originalPlaylist.add(song);
        MediaItem mediaItem = MediaItem.fromUri(song.getPath());
        exoPlayer.addMediaItem(mediaItem);
    }

    public void addToQueueNext(Song song) {
        if (currentPlaylist == null) currentPlaylist = new ArrayList<>();
        if (originalPlaylist == null) originalPlaylist = new ArrayList<>();

        int insertIndex;
        if (currentPlaylist.isEmpty() || currentIndex < 0) {
            insertIndex = 0;
        } else {
            insertIndex = Math.min(currentIndex + 1, currentPlaylist.size());
        }

        currentPlaylist.add(insertIndex, song);
        originalPlaylist.add(insertIndex, song);
        MediaItem mediaItem = MediaItem.fromUri(song.getPath());
        exoPlayer.addMediaItem(insertIndex, mediaItem);
    }

    public void removeFromQueue(int index) {
        if (index >= 0 && index < currentPlaylist.size()) {
            currentPlaylist.remove(index);
            originalPlaylist.remove(index);
            exoPlayer.removeMediaItem(index);

            if (index < currentIndex) {
                currentIndex--;
            }
        }
    }

    public void release() {
        saveCurrentState();
        stopPositionUpdates();
        exoPlayer.release();
        executorService.shutdown();
    }
}