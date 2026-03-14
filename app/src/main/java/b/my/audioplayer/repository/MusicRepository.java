package b.my.audioplayer.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import b.my.audioplayer.database.AppDatabase;
import b.my.audioplayer.database.SongDao;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.utils.Constants;
import java.util.List;

public class MusicRepository {

    private SongDao songDao;
    private MediatorLiveData<List<Song>> allSongs = new MediatorLiveData<>();
    private LiveData<List<Song>> currentSource;

    public MusicRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        songDao = database.songDao();
        setSortOrder(Constants.SORT_BY_TITLE);
    }

    public void setSortOrder(int sortOrder) {
        if (currentSource != null) {
            allSongs.removeSource(currentSource);
        }

        switch (sortOrder) {
            case Constants.SORT_BY_ARTIST:
                currentSource = songDao.getAllSongsByArtist();
                break;
            case Constants.SORT_BY_ALBUM:
                currentSource = songDao.getAllSongsByAlbum();
                break;
            case Constants.SORT_BY_DATE:
                currentSource = songDao.getAllSongsByDate();
                break;
            case Constants.SORT_BY_DURATION:
                currentSource = songDao.getAllSongsByDuration();
                break;
            case Constants.SORT_BY_TITLE:
            default:
                currentSource = songDao.getAllSongsByTitle();
                break;
        }

        allSongs.addSource(currentSource, songs -> allSongs.setValue(songs));
    }

    public void insert(Song song) {
        songDao.insert(song);
    }

    public void update(Song song) {
        songDao.update(song);
    }

    public void updateFavoriteStatus(long songId, boolean isFavorite) {
        songDao.updateFavoriteStatus(songId, isFavorite);
    }

    public void delete(Song song) {
        songDao.delete(song);
    }

    public LiveData<List<Song>> getAllSongs() {
        return allSongs;
    }

    public LiveData<List<Song>> getFavoriteSongs() {
        return songDao.getFavoriteSongs();
    }

    public LiveData<List<Song>> searchSongs(String query) {
        return songDao.searchSongs(query);
    }

    public LiveData<List<Song>> searchFavoriteSongs(String query) {
        return songDao.searchFavoriteSongs(query);
    }

    public LiveData<List<Song>> getSongsByAlbum(long albumId) {
        return songDao.getSongsByAlbum(albumId);
    }

    public LiveData<List<Song>> getSongsByArtist(String artistName) {
        return songDao.getSongsByArtist(artistName);
    }

    public LiveData<Song> getSongById(long songId) {
        return songDao.getSongById(songId);
    }
}