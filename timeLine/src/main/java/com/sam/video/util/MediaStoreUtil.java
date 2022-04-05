package com.sam.video.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class MediaStoreUtil {
    /**
     * 从image uri获取绝对路径
     *
     * @return 绝对路径
     */
    public static String imageUriToRealPath(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        Cursor cursor = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * video uri获取绝对路径
     *
     * @return 绝对路径
     */
    public static String videoUriToRealPath(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        Cursor cursor = null;
        String[] projection = {MediaStore.Video.Media.DATA};
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * audio uri获取绝对路径
     *
     * @return 绝对路径
     */
    public static String audioUriToRealPath(Context context, Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        Cursor cursor = null;
        String[] projection = {MediaStore.Audio.Media.DATA};
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
}
