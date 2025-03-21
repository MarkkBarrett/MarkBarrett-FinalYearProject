package com.example.vitalmix.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    // Method to get the actual file path from a URI
    public static String getPath(Context context, Uri uri) {
        String result = null;
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            if (cursor.moveToFirst()) {
                result = cursor.getString(columnIndex);
            }
            cursor.close();
        }

        if (result == null) {
            result = getFileFromUri(context, uri).getAbsolutePath();
        }

        return result;
    }

    // Fallback method to copy file to internal storage and return the path
    private static File getFileFromUri(Context context, Uri uri) {
        File file = new File(context.getCacheDir(), "temp_video.mp4");
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            Log.e("FileUtils", "Error copying file from URI", e);
        }
        return file;
    }
}