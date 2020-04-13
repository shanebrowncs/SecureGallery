package ca.shanebrown.securegallery;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;

public class ImageHandler {
    public static String[] getAllSecureImages() throws IOException {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();

        File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");

        boolean folder_exists = true;

        // Check and ensure we have permissions

        if(!secure_path.exists())
            folder_exists = secure_path.mkdirs();

        if(!folder_exists){
            Log.e("SecureGallery", "Could not create SecureGallery directory");
            throw new IOException("Could not create SecureGallery directory");
        }

        File[] files = secure_path.listFiles();
        String[] paths = new String[files.length];
        for(int i = 0; i < paths.length; i++){
            paths[i] = files[i].getAbsolutePath();
        }

        return paths;
    }

    public static boolean requestWritePermissions(Activity act){
        if(Build.VERSION.SDK_INT >= 23) {
            if (act.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE} ,1);
                return false; // Return false if we didn't have the permission and we had to request it.
            }
        }else{
            Log.e("SecureGallery", "SDK < 23, requesting permissions not needed");
        }

        return true;
    }
}
