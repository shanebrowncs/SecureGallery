package ca.shanebrown.securegallery;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static java.lang.System.in;
import static java.lang.System.out;

public class ImageHandler {
    public static String[] getAllSecureImagePaths() throws IOException {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();

        File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");

        boolean folder_exists = true;

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

    public static Bitmap[] getAllSecureImageBitmaps(String key, byte[] salt) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException {
        String[] images = getAllSecureImagePaths();
        Bitmap[] bmaps = new Bitmap[images.length];

        for(int i = 0; i < images.length; i++){
            bmaps[i] = decryptImage(new File(images[i]), key, salt);
        }

        return bmaps;
    }

    public static void secureImagesByURIs(ArrayList<Uri> uris, String key, byte[] salt) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException, InvalidKeySpecException {
        for(Uri cur_uri : uris){
            File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");
            File cur_file = new File(cur_uri.getPath());
            File dest_file = new File(secure_path + File.separator + cur_file.getName());

            Log.e("SecureGallery", "Encrypting " + cur_file.getName());
            encryptNewFile(cur_file, dest_file, key, salt);
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

    public static Bitmap decryptImage(File file, String pw, byte[] salt) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        // Generate key from PW
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        IvParameterSpec iv = new IvParameterSpec(keyBytes);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

        CipherInputStream inStream = new CipherInputStream(new FileInputStream(file), cipher);

        Bitmap bmap = BitmapFactory.decodeStream(inStream);

        inStream.close();
        return bmap;
    }

    public static void encryptNewFile(File src, File dest, String pw, byte[] salt) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException {
        // Generate key from PW
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        IvParameterSpec iv = new IvParameterSpec(keyBytes);

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

        int count = 0;
        byte[] buff = new byte[1024];


        InputStream inStream = new FileInputStream(src);
        CipherOutputStream outStream = new CipherOutputStream(new FileOutputStream(dest), cipher);

        try {
            while ((count = inStream.read(buff)) > 0) {
                outStream.write(buff);
            }
        }finally {
            outStream.close();
        }
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
