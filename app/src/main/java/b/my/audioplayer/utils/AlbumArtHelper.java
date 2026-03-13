package b.my.audioplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.FileDescriptor;

public class AlbumArtHelper {

    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");

    public static Bitmap getAlbumArt(Context context, long albumId) {
        Bitmap albumArt = null;
        try {
            Uri uri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId);
            ContentResolver resolver = context.getContentResolver();
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");

            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                albumArt = BitmapFactory.decodeFileDescriptor(fd);
                pfd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return albumArt;
    }

    public static Bitmap getAlbumArtFromFile(String filePath) {
        Bitmap albumArt = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(filePath);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                albumArt = BitmapFactory.decodeByteArray(art, 0, art.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return albumArt;
    }

    public static Bitmap getScaledBitmap(Bitmap bitmap, int width, int height) {
        if (bitmap == null) return null;
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}