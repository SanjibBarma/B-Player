package b.my.audioplayer.video_player.model;

import android.net.Uri;

public class FolderModel {
    private String name;
    private int videoCount;
    private Uri thumbnailUri;

    public FolderModel(String name, int videoCount, Uri thumbnailUri) {
        this.name = name;
        this.videoCount = videoCount;
        this.thumbnailUri = thumbnailUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public Uri getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(Uri thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public String getVideoCountText() {
        return videoCount + (videoCount == 1 ? " Video" : " Videos");
    }
}