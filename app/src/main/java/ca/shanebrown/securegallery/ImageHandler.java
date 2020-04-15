package ca.shanebrown.securegallery;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class ImageHandler {
    public static String[] getAllSecureImagePaths() throws IOException {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();

        File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");

        boolean folder_exists = true;

        if(!secure_path.exists()) {
            folder_exists = secure_path.mkdirs();
            File nomedia = new File(secure_path + File.separator + ".nomedia");
            nomedia.createNewFile();
        }

        if(!folder_exists){
            Log.e("SecureGallery", "Could not create SecureGallery directory");
            throw new IOException("Could not create SecureGallery directory");
        }

        // List files, exclude .nomedia file
        File[] files = secure_path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.getName().equals(".nomedia");
            }
        });
        String[] paths = new String[files.length];
        for(int i = 0; i < paths.length; i++){
            paths[i] = files[i].getAbsolutePath();
        }

        return paths;
    }

    public static ImagePathBundle[] getAllSecureImageBitmaps(String key, byte[] salt) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        String[] images = getAllSecureImagePaths();
        ImagePathBundle[] imageBundle = new ImagePathBundle[images.length];

        for(int i = 0; i < images.length; i++){
            Bitmap bmap = decryptImage(new File(images[i]), key, salt);
            imageBundle[i] = new ImagePathBundle(images[i], bmap);
        }

        return imageBundle;
    }

    public static void secureImagesByURIs(ArrayList<Uri> uris, String key, byte[] salt) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException {
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

    public static boolean deleteFile(File file){
        if(file.exists()){
            return file.delete();
        }

        return false;
    }

    public static void saveDecryptedImage(File file, String pw, byte[] salt) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, IOException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // READ IN ENCRYPTED DATA
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        HashMap<String, byte[]> map = (HashMap<String, byte[]>)ois.readObject();
        ois.close();
        fis.close();

        byte[] iv = map.get("iv");
        byte[] cipherBytes = map.get("cipherBytes");

        // DECRYPTION OF READ DATA
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] clearBytes = cipher.doFinal(cipherBytes);

        File export_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + file.getName());

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(export_path));
        bos.write(clearBytes);
        bos.flush();
        bos.close();
    }

    public static Bitmap decryptImage(File file, String pw, byte[] salt) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // READ IN ENCRYPTED DATA
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        HashMap<String, byte[]> map = (HashMap<String, byte[]>)ois.readObject();
        ois.close();
        fis.close();

        byte[] iv = map.get("iv");
        byte[] cipherBytes = map.get("cipherBytes");

        // DECRYPTION OF READ DATA
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] clearBytes = cipher.doFinal(cipherBytes);
        return BitmapFactory.decodeByteArray(clearBytes, 0, clearBytes.length);
    }

    public static void encryptNewFile(File src, File dest, String pw, byte[] salt) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException {
        // Generate key from PW
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        SecretKey key = new SecretKeySpec(keyBytes, "AES");

        // Setup cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[cipher.getBlockSize()];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);

        // Read in clear bytes
        int fileSize = (int)src.length();
        byte[] clearBytes = new byte[fileSize];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(src));
        bis.read(clearBytes, 0, fileSize);
        bis.close();

        // Generate ciphertext
        byte[] cipherBytes = cipher.doFinal(clearBytes);

        HashMap<String, byte[]> map = new HashMap<>();
        map.put("iv", iv);
        map.put("cipherBytes", cipherBytes);

        // WRITE CIPHERTEXT TO FILE
        FileOutputStream fos = new FileOutputStream(dest);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        oos.writeObject(map);
        oos.flush();
        oos.close();
        fos.close();
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
