package com.letv.android.recorder.tool;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerClient;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;

/**
 * Created by snile on 14/11/3.
 */
public class FileSyncContentProvider {

    public static void deleteFile(Context context, String filePath) {
        removeImageFromLib(context,filePath);
    }


    public static void renameFile(Context context, String oldFile, String newFile) {
        removeImageFromLib(context,oldFile);
        scanFile(context,new File(newFile));

    }

    public static int removeImageFromLib(Context context, String filePath) {
        ContentResolver resolver = context.getContentResolver();
        return resolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", new String[]{filePath});
    }

    public static void scanFile(final  Context context, File newFile) {
        MediaScannerConnection.scanFile(context,new String[]{newFile.getAbsolutePath()},null,new MediaScannerConnection.OnScanCompletedListener(){
            @Override
            public void onScanCompleted(String path, Uri uri) {
                if (uri!=null){
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, "0");
                    context.getContentResolver().update(uri, cv, "is_music = ?", new String[]{"1"});
                }
            }
        });
    }

}
