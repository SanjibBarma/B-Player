package b.my.audioplayer.audio_player.viewmodel;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import b.my.audioplayer.audio_player.model.Album;
import b.my.audioplayer.audio_player.model.Artist;
import b.my.audioplayer.audio_player.model.Song;
import b.my.audioplayer.audio_player.repository.MusicRepository;
import b.my.audioplayer.utils.Constants;
import b.my.audioplayer.utils.MediaScanner;
import java.util.ArrayList;
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

    // Store ringtones count for album display
    private int ringtonesCount = 0;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new MusicRepository(application);

        // Filter songs - only duration >= 1 minute
        allSongs = Transformations.switchMap(searchQuery, query -> {
            LiveData<List<Song>> sourceSongs;
            if (query == null || query.isEmpty()) {
                sourceSongs = repository.getAllSongs();
            } else {
                sourceSongs = repository.searchSongs("%" + query + "%");
            }

            return Transformations.map(sourceSongs, songs -> {
                if (songs == null) return new ArrayList<>();

                List<Song> filteredSongs = new ArrayList<>();
                for (Song song : songs) {
                    if (song.getDuration() >= Constants.ONE_MINUTE_MS) {
                        filteredSongs.add(song);
                    }
                }
                return filteredSongs;
            });
        });

        // Filter favorite songs - only duration >= 1 minute
        favoriteSongs = Transformations.switchMap(searchQuery, query -> {
            LiveData<List<Song>> sourceSongs;
            if (query == null || query.isEmpty()) {
                sourceSongs = repository.getFavoriteSongs();
            } else {
                sourceSongs = repository.searchFavoriteSongs("%" + query + "%");
            }

            return Transformations.map(sourceSongs, songs -> {
                if (songs == null) return new ArrayList<>();

                List<Song> filteredSongs = new ArrayList<>();
                for (Song song : songs) {
                    if (song.getDuration() >= Constants.ONE_MINUTE_MS) {
                        filteredSongs.add(song);
                    }
                }
                return filteredSongs;
            });
        });

        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<Song>> getAllSongs() {
        return allSongs;
    }

    public LiveData<List<Song>> getFavoriteSongs() {
        return favoriteSongs;
    }

    // Get ringtones (duration < 1 minute)
    public LiveData<List<Song>> getRingtones() {
        return Transformations.map(repository.getAllSongs(), songs -> {
            if (songs == null) return new ArrayList<>();

            List<Song> ringtones = new ArrayList<>();
            for (Song song : songs) {
                if (song.getDuration() < Constants.ONE_MINUTE_MS) {
                    ringtones.add(song);
                }
            }
            return ringtones;
        });
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

            // Count ringtones (duration < 1 minute)
            int ringtoneCount = 0;
            for (Song song : songs) {
                if (song.getDuration() < Constants.ONE_MINUTE_MS) {
                    ringtoneCount++;
                }
            }
            ringtonesCount = ringtoneCount;

            // Insert songs to database
            for (Song song : songs) {
                repository.insert(song);
            }

            // Scan albums and artists
            List<Album> albums = MediaScanner.scanAlbums(context);
            List<Artist> artists = MediaScanner.scanArtists(context);

            // Add Ringtones virtual album if there are ringtones
            if (ringtonesCount > 0) {
                Album ringtonesAlbum = new Album(Constants.RINGTONE_ALBUM_ID, "Ringtones", "Various");
                ringtonesAlbum.setSongCount(ringtonesCount);
                ringtonesAlbum.setAlbumArt(null); // Will use default icon
                albums.add(0, ringtonesAlbum); // Add at the beginning
            }

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
        boolean newStatus = !song.isFavorite();
        song.setFavorite(newStatus);
        executorService.execute(() -> repository.updateFavoriteStatus(song.getId(), newStatus));
    }

    public LiveData<List<Song>> searchSongs(String query) {
        return repository.searchSongs("%" + query + "%");
    }

    public LiveData<List<Song>> getSongsByAlbum(long albumId) {
        // Check if it's the Ringtones virtual album
        if (albumId == Constants.RINGTONE_ALBUM_ID) {
            return getRingtones();
        }

        // Filter normal album songs to only include duration >= 1 minute
        return Transformations.map(repository.getSongsByAlbum(albumId), songs -> {
            if (songs == null) return new ArrayList<>();

            List<Song> filteredSongs = new ArrayList<>();
            for (Song song : songs) {
                if (song.getDuration() >= Constants.ONE_MINUTE_MS) {
                    filteredSongs.add(song);
                }
            }
            return filteredSongs;
        });
    }

    public LiveData<List<Song>> getSongsByArtist(String artistName) {
        // Filter artist songs to only include duration >= 1 minute
        return Transformations.map(repository.getSongsByArtist(artistName), songs -> {
            if (songs == null) return new ArrayList<>();

            List<Song> filteredSongs = new ArrayList<>();
            for (Song song : songs) {
                if (song.getDuration() >= Constants.ONE_MINUTE_MS) {
                    filteredSongs.add(song);
                }
            }
            return filteredSongs;
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}