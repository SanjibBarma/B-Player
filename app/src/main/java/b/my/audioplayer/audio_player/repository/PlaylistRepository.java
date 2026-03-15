package b.my.audioplayer.audio_player.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import b.my.audioplayer.audio_player.database.AppDatabase;
import b.my.audioplayer.audio_player.database.PlaylistDao;
import b.my.audioplayer.audio_player.database.PlaylistSongDao;
import b.my.audioplayer.audio_player.model.Playlist;
import b.my.audioplayer.audio_player.model.PlaylistSong;
import b.my.audioplayer.audio_player.model.Song;
import java.util.List;

public class PlaylistRepository {

    private PlaylistDao playlistDao;
    private PlaylistSongDao playlistSongDao;
    private LiveData<List<Playlist>> allPlaylists;

    public PlaylistRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        playlistDao = database.playlistDao();
        playlistSongDao = database.playlistSongDao();
        allPlaylists = playlistDao.getAllPlaylists();
    }

    // Playlist CRUD operations
    public long insert(Playlist playlist) {
        return playlistDao.insert(playlist);
    }

    public void update(Playlist playlist) {
        playlistDao.update(playlist);
    }

    public void delete(Playlist playlist) {
        playlistDao.delete(playlist);
    }

    public LiveData<List<Playlist>> getAllPlaylists() {
        return allPlaylists;
    }

    public LiveData<Playlist> getPlaylistById(long playlistId) {
        return playlistDao.getPlaylistById(playlistId);
    }

    public void renamePlaylist(long playlistId, String newName) {
        playlistDao.renamePlaylist(playlistId, newName);
    }

    public void updatePlaylistSongCount(long playlistId, int count) {
        playlistDao.updateSongCount(playlistId, count);
    }

    // Playlist-Song relationship operations
    public LiveData<List<Song>> getPlaylistSongs(long playlistId) {
        return playlistSongDao.getSongsInPlaylist(playlistId);
    }

    public void addSongToPlaylist(PlaylistSong playlistSong) {
        playlistSongDao.insert(playlistSong);
    }

    public void removeSongFromPlaylist(long playlistId, long songId) {
        playlistSongDao.removeSongFromPlaylist(playlistId, songId);
    }

    public int getSongCountInPlaylist(long playlistId) {
        return playlistSongDao.getSongCountInPlaylist(playlistId);
    }

    public boolean isSongInPlaylist(long playlistId, long songId) {
        return playlistSongDao.isSongInPlaylist(playlistId, songId) > 0;
    }

    public void clearPlaylist(long playlistId) {
        playlistSongDao.clearPlaylist(playlistId);
    }

    public void reorderSongs(long playlistId, List<PlaylistSong> newOrder) {
        for (int i = 0; i < newOrder.size(); i++) {
            playlistSongDao.updateSongOrder(playlistId, newOrder.get(i).getSongId(), i);
        }
    }
}