package b.my.audioplayer.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;

public class FileUtils {

    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    public static boolean renameFile(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        return oldFile.exists() && oldFile.renameTo(newFile);
    }

    public static String getFileExtension(String filePath) {
        if (filePath == null) return "";
        int lastDot = filePath.lastIndexOf(".");
        if (lastDot >= 0) {
            return filePath.substring(lastDot).toLowerCase();
        }
        return "";
    }

    public static boolean isSupportedFormat(String filePath) {
        String extension = getFileExtension(filePath);
        for (String format : Constants.SUPPORTED_FORMATS) {
            if (format.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.2f %s",
                size / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    public static String getFileName(String filePath) {
        if (filePath == null) return "";
        int lastSlash = filePath.lastIndexOf("/");
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            return filePath.substring(lastSlash + 1);
        }
        return filePath;
    }

    public static String getFileNameWithoutExtension(String filePath) {
        String fileName = getFileName(filePath);
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot >= 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    public static String getParentDirectory(String filePath) {
        if (filePath == null) return "";
        int lastSlash = filePath.lastIndexOf("/");
        if (lastSlash >= 0) {
            return filePath.substring(0, lastSlash);
        }
        return "";
    }

    public static void deleteFromMediaStore(Context context, String filePath) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.DATA + "=?";
        String[] selectionArgs = {filePath};
        resolver.delete(uri, selection, selectionArgs);
    }
}