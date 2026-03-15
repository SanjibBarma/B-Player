package b.my.audioplayer.audio_player.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import b.my.audioplayer.audio_player.model.Song;
import java.util.List;

@Dao
public interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Song song);

    @Update
    void update(Song song);

    @Delete
    void delete(Song song);

    @Query("SELECT * FROM songs ORDER BY title ASC")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs ORDER BY title ASC")
    List<Song> getAllSongsSync();

    @Query("SELECT * FROM songs ORDER BY title ASC")
    LiveData<List<Song>> getAllSongsByTitle();

    @Query("SELECT * FROM songs ORDER BY artist ASC")
    LiveData<List<Song>> getAllSongsByArtist();

    @Query("SELECT * FROM songs ORDER BY album ASC")
    LiveData<List<Song>> getAllSongsByAlbum();

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    LiveData<List<Song>> getAllSongsByDate();

    @Query("SELECT * FROM songs ORDER BY duration DESC")
    LiveData<List<Song>> getAllSongsByDuration();

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    LiveData<List<Song>> getFavoriteSongs();

    @Query("SELECT * FROM songs WHERE title LIKE :query OR artist LIKE :query OR album LIKE :query ORDER BY title ASC")
    LiveData<List<Song>> searchSongs(String query);

    @Query("SELECT * FROM songs WHERE isFavorite = 1 AND (title LIKE :query OR artist LIKE :query OR album LIKE :query) ORDER BY title ASC")
    LiveData<List<Song>> searchFavoriteSongs(String query);

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY title ASC")
    LiveData<List<Song>> getSongsByAlbum(long albumId);

    @Query("SELECT * FROM songs WHERE artist = :artistName ORDER BY title ASC")
    LiveData<List<Song>> getSongsByArtist(String artistName);

    @Query("SELECT * FROM songs WHERE id = :songId")
    LiveData<Song> getSongById(long songId);

    @Query("SELECT * FROM songs WHERE id = :songId")
    Song getSongByIdSync(long songId);

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    LiveData<List<Song>> getRecentlyAdded();

    @Query("SELECT COUNT(*) FROM songs")
    int getSongCount();

    @Query("DELETE FROM songs")
    void deleteAll();

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    void updateFavoriteStatus(long songId, boolean isFavorite);
}