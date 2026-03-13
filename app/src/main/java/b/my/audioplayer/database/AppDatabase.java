package b.my.audioplayer.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import b.my.audioplayer.model.Playlist;
import b.my.audioplayer.model.PlaylistSong;
import b.my.audioplayer.model.Song;

@Database(entities = {Song.class, Playlist.class, PlaylistSong.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();
    public abstract PlaylistSongDao playlistSongDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "bplayer_database"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}