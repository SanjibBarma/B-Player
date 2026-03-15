package b.my.audioplayer.video_player.model;

import android.net.Uri;

public class VideoModel {
    private long id;
    private String title;
    private String path;
    private long duration;
    private long size;
    private String folderName;
    private int width;
    private int height;
    private Uri uri;

    public VideoModel() {}

    public VideoModel(long id, String title, String path, long duration,
                      long size, String folderName, int width, int height, Uri uri) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.duration = duration;
        this.size = size;
        this.folderName = folderName;
        this.width = width;
        this.height = height;
        this.uri = uri;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public Uri getUri() { return uri; }
    public void setUri(Uri uri) { this.uri = uri; }

    // Helper methods
    public String getFormattedDuration() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public String getFormattedSize() {
        double kb = size / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;

        if (gb >= 1) {
            return String.format("%.2f GB", gb);
        } else if (mb >= 1) {
            return String.format("%.1f MB", mb);
        } else {
            return String.format("%.0f KB", kb);
        }
    }

    public String getQualityTag() {
        if (height >= 2160) return "4K";
        if (height >= 1080) return "FHD";
        if (height >= 720) return "HD";
        return null;
    }

    public boolean isHD() {
        return height >= 720;
    }
}