package b.my.audioplayer.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import b.my.audioplayer.model.Playlist;
import b.my.audioplayer.model.PlaylistSong;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.repository.PlaylistRepository;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistViewModel extends AndroidViewModel {

    private PlaylistRepository repository;
    private LiveData<List<Playlist>> allPlaylists;
    private ExecutorService executorService;

    public PlaylistViewModel(@NonNull Application application) {
        super(application);
        repository = new PlaylistRepository(application);
        allPlaylists = repository.getAllPlaylists();
        executorService = Executors.newSingleThreadExecutor();
    }

    // Get all playlists
    public LiveData<List<Playlist>> getAllPlaylists() {
        return allPlaylists;
    }

    // Get playlist by ID
    public LiveData<Playlist> getPlaylistById(long playlistId) {
        return repository.getPlaylistById(playlistId);
    }

    // Get songs in a specific playlist
    public LiveData<List<Song>> getPlaylistSongs(long playlistId) {
        return repository.getPlaylistSongs(playlistId);
    }

    // Create new playlist
    public void createPlaylist(String name) {
        Playlist playlist = new Playlist(name);
        executorService.execute(() -> repository.insert(playlist));
    }

    // Create playlist and return ID
    public void createPlaylist(String name, OnPlaylistCreatedListener listener) {
        Playlist playlist = new Playlist(name);
        executorService.execute(() -> {
            long id = repository.insert(playlist);
            if (listener != null) {
                listener.onPlaylistCreated(id);
            }
        });
    }

    // Update playlist
    public void updatePlaylist(Playlist playlist) {
        executorService.execute(() -> repository.update(playlist));
    }

    // Delete playlist
    public void deletePlaylist(Playlist playlist) {
        executorService.execute(() -> repository.delete(playlist));
    }

    // Rename playlist
    public void renamePlaylist(long playlistId, String newName) {
        executorService.execute(() -> repository.renamePlaylist(playlistId, newName));
    }

    // Add song to playlist
    public void addSongToPlaylist(long playlistId, long songId) {
        executorService.execute(() -> {
            int currentCount = repository.getSongCountInPlaylist(playlistId);
            PlaylistSong playlistSong = new PlaylistSong(playlistId, songId, currentCount);
            repository.addSongToPlaylist(playlistSong);
            repository.updatePlaylistSongCount(playlistId, currentCount + 1);
        });
    }

    // Add multiple songs to playlist
    public void addSongsToPlaylist(long playlistId, List<Long> songIds) {
        executorService.execute(() -> {
            int currentCount = repository.getSongCountInPlaylist(playlistId);
            for (int i = 0; i < songIds.size(); i++) {
                PlaylistSong playlistSong = new PlaylistSong(playlistId, songIds.get(i), currentCount + i);
                repository.addSongToPlaylist(playlistSong);
            }
            repository.updatePlaylistSongCount(playlistId, currentCount + songIds.size());
        });
    }

    // Remove song from playlist
    public void removeSongFromPlaylist(long playlistId, long songId) {
        executorService.execute(() -> {
            repository.removeSongFromPlaylist(playlistId, songId);
            int newCount = repository.getSongCountInPlaylist(playlistId);
            repository.updatePlaylistSongCount(playlistId, newCount);
        });
    }

    // Check if song is in playlist
    public void isSongInPlaylist(long playlistId, long songId, OnCheckResultListener listener) {
        executorService.execute(() -> {
            boolean isInPlaylist = repository.isSongInPlaylist(playlistId, songId);
            if (listener != null) {
                listener.onResult(isInPlaylist);
            }
        });
    }

    // Clear all songs from playlist
    public void clearPlaylist(long playlistId) {
        executorService.execute(() -> {
            repository.clearPlaylist(playlistId);
            repository.updatePlaylistSongCount(playlistId, 0);
        });
    }

    // Callback interfaces
    public interface OnPlaylistCreatedListener {
        void onPlaylistCreated(long playlistId);
    }

    public interface OnCheckResultListener {
        void onResult(boolean result);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}