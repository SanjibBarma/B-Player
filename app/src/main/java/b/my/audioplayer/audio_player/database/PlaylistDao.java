package b.my.audioplayer.audio_player.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import b.my.audioplayer.audio_player.model.Playlist;
import java.util.List;

@Dao
public interface PlaylistDao {

    @Insert
    long insert(Playlist playlist);

    @Update
    void update(Playlist playlist);

    @Delete
    void delete(Playlist playlist);

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    LiveData<List<Playlist>> getAllPlaylists();

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    LiveData<Playlist> getPlaylistById(long playlistId);

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    Playlist getPlaylistByIdSync(long playlistId);

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    void renamePlaylist(long playlistId, String newName);

    @Query("UPDATE playlists SET songCount = :count WHERE id = :playlistId")
    void updateSongCount(long playlistId, int count);

    @Query("SELECT COUNT(*) FROM playlists")
    int getPlaylistCount();

    @Query("DELETE FROM playlists")
    void deleteAll();
}