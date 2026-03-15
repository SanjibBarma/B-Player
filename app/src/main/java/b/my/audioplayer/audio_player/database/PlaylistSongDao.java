package b.my.audioplayer.audio_player.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import b.my.audioplayer.audio_player.model.PlaylistSong;
import b.my.audioplayer.audio_player.model.Song;
import java.util.List;

@Dao
public interface PlaylistSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PlaylistSong playlistSong);

    @Delete
    void delete(PlaylistSong playlistSong);

    @Query("SELECT s.* FROM songs s " +
            "INNER JOIN playlist_songs ps ON s.id = ps.songId " +
            "WHERE ps.playlistId = :playlistId " +
            "ORDER BY ps.orderIndex ASC")
    LiveData<List<Song>> getSongsInPlaylist(long playlistId);

    @Query("SELECT s.* FROM songs s " +
            "INNER JOIN playlist_songs ps ON s.id = ps.songId " +
            "WHERE ps.playlistId = :playlistId " +
            "ORDER BY ps.orderIndex ASC")
    List<Song> getSongsInPlaylistSync(long playlistId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    void removeSongFromPlaylist(long playlistId, long songId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    int getSongCountInPlaylist(long playlistId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    int isSongInPlaylist(long playlistId, long songId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void clearPlaylist(long playlistId);

    @Query("UPDATE playlist_songs SET orderIndex = :newOrder WHERE playlistId = :playlistId AND songId = :songId")
    void updateSongOrder(long playlistId, long songId, int newOrder);

    @Query("SELECT MAX(orderIndex) FROM playlist_songs WHERE playlistId = :playlistId")
    int getMaxOrderIndex(long playlistId);

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    List<PlaylistSong> getPlaylistSongsRelation(long playlistId);
}