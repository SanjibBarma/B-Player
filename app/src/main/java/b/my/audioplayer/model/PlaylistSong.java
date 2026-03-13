package b.my.audioplayer.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "playlist_songs",
        primaryKeys = {"playlistId", "songId"},
        foreignKeys = {
                @ForeignKey(entity = Playlist.class,
                        parentColumns = "id",
                        childColumns = "playlistId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Song.class,
                        parentColumns = "id",
                        childColumns = "songId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("playlistId"), @Index("songId")})
public class PlaylistSong {
    private long playlistId;
    private long songId;
    private int orderIndex;

    public PlaylistSong(long playlistId, long songId, int orderIndex) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.orderIndex = orderIndex;
    }

    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }

    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}