package ca.shanebrown.securegallery;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.System.in;

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

    public static void secureImagesByURIs(ArrayList<Uri> uris){
        for(Uri cur_uri : uris){
            File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");
            File cur_file = new File(cur_uri.getPath());
            File dest_file = new File(secure_path + File.separator + cur_file.getName());

            // Copy file to secure dir, if a failure occurs abort all so user can figure out what happened
            if(!copyFile(cur_file, dest_file)){
                Log.e("SecureGallery", "Fatal: Failed to copy file: " + cur_file.getName());
                return;
            }
        }
    }

    public static boolean copyFile(File src, File dest){
        try{
            FileInputStream fis = new FileInputStream(src);

            FileOutputStream fos = new FileOutputStream(dest);
            byte[] buff = new byte[1024];
            int len;

            try {
                while ((len = fis.read(buff)) > 0) {
                    fos.write(buff, 0, len);
                }

                // Free file resources
                fos.close();
                fis.close();
            }catch(IOException ex){
                Log.e("SecureGallery", "IOException while writing dest file");
                return false;
            }
        }catch(FileNotFoundException ex){
            Log.e("SecureGallery", "File to copy not found");
            return false;
        }

        return true;
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
