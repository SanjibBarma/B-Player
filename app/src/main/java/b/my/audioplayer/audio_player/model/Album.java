package b.my.audioplayer.audio_player.model;

public class Album {
    private long id;
    private String name;
    private String artist;
    private String albumArt;
    private int songCount;
    private int year;

    public Album(long id, String name, String artist) {
        this.id = id;
        this.name = name;
        this.artist = artist;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbumArt() { return albumArt; }
    public void setAlbumArt(String albumArt) { this.albumArt = albumArt; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}