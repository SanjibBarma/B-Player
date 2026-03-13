package b.my.audioplayer.viewmodel;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import b.my.audioplayer.database.AppDatabase;
import b.my.audioplayer.model.Album;
import b.my.audioplayer.model.Artist;
import b.my.audioplayer.model.Song;
import b.my.audioplayer.repository.MusicRepository;
import b.my.audioplayer.utils.MediaScanner;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    private MusicRepository repository;
    private LiveData<List<Song>> allSongs;
    private LiveData<List<Song>> favoriteSongs;
    private MutableLiveData<List<Album>> allAlbums = new MutableLiveData<>();
    private MutableLiveData<List<Artist>> allArtists = new MutableLiveData<>();
    private MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);
    private MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private ExecutorService executorService;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new MusicRepository(application);
        
        allSongs = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return repository.getAllSongs();
            } else {
                return repository.searchSongs("%" + query + "%");
            }
        });

        favoriteSongs = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.isEmpty()) {
                return repository.getFavoriteSongs();
            } else {
                return repository.searchFavoriteSongs("%" + query + "%");
            }
        });

        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Song>> getAllSongs() {
        return allSongs;
    }

    public LiveData<List<Song>> getFavoriteSongs() {
        return favoriteSongs;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void setSortOrder(int sortOrder) {
        repository.setSortOrder(sortOrder);
    }

    public LiveData<List<Album>> getAllAlbums() {
        return allAlbums;
    }

    public LiveData<List<Artist>> getAllArtists() {
        return allArtists;
    }

    public LiveData<Boolean> getIsScanning() {
        return isScanning;
    }

    public void scanMediaFiles(Context context) {
        isScanning.setValue(true);
        executorService.execute(() -> {
            List<Song> songs = MediaScanner.scanAudioFiles(context);

            // Insert songs to database
            for (Song song : songs) {
                repository.insert(song);
            }

            // Scan albums and artists
            List<Album> albums = MediaScanner.scanAlbums(context);
            List<Artist> artists = MediaScanner.scanArtists(context);

            allAlbums.postValue(albums);
            allArtists.postValue(artists);
            isScanning.postValue(false);
        });
    }

    public void insertSong(Song song) {
        executorService.execute(() -> repository.insert(song));
    }

    public void updateSong(Song song) {
        executorService.execute(() -> repository.update(song));
    }

    public void deleteSong(Song song) {
        executorService.execute(() -> repository.delete(song));
    }

    public void toggleFavorite(Song song) {
        song.setFavorite(!song.isFavorite());
        executorService.execute(() -> repository.update(song));
    }

    public LiveData<List<Song>> searchSongs(String query) {
        return repository.searchSongs("%" + query + "%");
    }

    public LiveData<List<Song>> getSongsByAlbum(long albumId) {
        return repository.getSongsByAlbum(albumId);
    }

    public LiveData<List<Song>> getSongsByArtist(String artistName) {
        return repository.getSongsByArtist(artistName);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}