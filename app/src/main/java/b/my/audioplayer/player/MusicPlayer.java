package b.my.audioplayer.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.PreferenceHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                if (newIndex >= 0) {
                    currentIndex = newIndex;
                }
                if (callback != null && currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
                    callback.onSongChanged(currentPlaylist.get(currentIndex));
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

    public void setPlaylist(List<Song> songs) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPlaylist = new ArrayList<>(songs);
        currentIndex = 0;

        if (isShuffleEnabled) {
            shufflePlaylist();
        }

        preparePlaylist();
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPlaylist = new ArrayList<>(songs);

        // Ensure startIndex is valid
        if (startIndex < 0 || startIndex >= currentPlaylist.size()) {
            this.currentIndex = 0;
        } else {
            this.currentIndex = startIndex;
        }

        if (isShuffleEnabled) {
            Song selectedSong = currentPlaylist.get(currentIndex);
            shufflePlaylist();
            // Find the selected song in the shuffled list
            this.currentIndex = currentPlaylist.indexOf(selectedSong);
            if (currentIndex < 0) {
                this.currentIndex = 0;
            }
        }

        preparePlaylist();
    }

    private void preparePlaylist() {
        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : currentPlaylist) {
            MediaItem mediaItem = MediaItem.fromUri(song.getPath());
            mediaItems.add(mediaItem);
        }
        
        // Use atomic setMediaItems to ensure correct starting song and avoid race conditions
        exoPlayer.setMediaItems(mediaItems, currentIndex, 0);
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

        // If playlist is empty, don't try to play
        if (currentPlaylist == null || currentPlaylist.isEmpty()) {
            if (callback != null) {
                callback.onPlaybackError("Playlist is empty");
            }
            return;
        }

        // Ensure we have a valid index
        if (currentIndex < 0 || currentIndex >= currentPlaylist.size()) {
            currentIndex = 0;
        }

        // Ensure ExoPlayer is at the correct position before playing
        if (exoPlayer.getCurrentMediaItemIndex() != currentIndex) {
            exoPlayer.seekToDefaultPosition(currentIndex);
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
        if (currentIndex < currentPlaylist.size() - 1) {
            currentIndex++;
            exoPlayer.seekToDefaultPosition(currentIndex);
            play();
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            currentIndex = 0;
            exoPlayer.seekToDefaultPosition(currentIndex);
            play();
        }
    }

    public void playPrevious() {
        if (exoPlayer.getCurrentPosition() > 3000) {
            exoPlayer.seekTo(0);
        } else if (currentIndex > 0) {
            currentIndex--;
            exoPlayer.seekToDefaultPosition(currentIndex);
            play();
        } else if (repeatMode == Player.REPEAT_MODE_ALL) {
            currentIndex = currentPlaylist.size() - 1;
            exoPlayer.seekToDefaultPosition(currentIndex);
            play();
        }
    }

    public void playSongAt(int index) {
        if (index >= 0 && index < currentPlaylist.size()) {
            currentIndex = index;
            exoPlayer.seekToDefaultPosition(currentIndex);
            play();
        }
    }

    public void seekTo(long position) {
        exoPlayer.seekTo(position);
    }

    public void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;

        if (isShuffleEnabled) {
            shufflePlaylist();
        } else {
            Song currentSong = getCurrentSong();
            currentPlaylist = new ArrayList<>(originalPlaylist);
            if (currentSong != null) {
                currentIndex = currentPlaylist.indexOf(currentSong);
                if (currentIndex < 0) currentIndex = 0;
            }
        }

        preparePlaylist();
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
            if (currentIndex >= currentPlaylist.size() - 1) {
                // Playlist ended
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
        if (currentPlaylist == null) {
            currentPlaylist = new ArrayList<>();
        }
        if (originalPlaylist == null) {
            originalPlaylist = new ArrayList<>();
        }

        // If no song is currently playing, add at the beginning
        if (currentIndex < 0 || currentIndex >= currentPlaylist.size()) {
            addToQueue(song);
            return;
        }

        int insertIndex = currentIndex + 1;

        // Ensure insertIndex doesn't exceed list size
        if (insertIndex > currentPlaylist.size()) {
            insertIndex = currentPlaylist.size();
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
    }
}