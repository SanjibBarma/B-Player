package b.my.audioplayer.audio_player.model;

public class Artist {
    private long id;
    private String name;
    private int songCount;
    private int albumCount;

    public Artist(long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }

    public int getAlbumCount() { return albumCount; }
    public void setAlbumCount(int albumCount) { this.albumCount = albumCount; }
}