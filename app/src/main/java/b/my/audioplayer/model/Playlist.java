package b.my.audioplayer.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "playlists")
public class Playlist implements Serializable {

    private static final long serialVersionUID = 1L;

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private long createdAt;
    private long modifiedAt;
    private int songCount;
    private String coverArtPath;

    public Playlist() {}

    public Playlist(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = System.currentTimeMillis();
        this.songCount = 0;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.modifiedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(long modifiedAt) { this.modifiedAt = modifiedAt; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) {
        this.songCount = songCount;
        this.modifiedAt = System.currentTimeMillis();
    }

    public String getCoverArtPath() { return coverArtPath; }
    public void setCoverArtPath(String coverArtPath) { this.coverArtPath = coverArtPath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return id == playlist.id;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", songCount=" + songCount +
                '}';
    }
}