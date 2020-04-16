package ca.shanebrown.securegallery;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

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
    public static final int KEY_ITERATIONS = 10000;

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

    public static void secureImagesByURIs(Context context, ArrayList<Uri> uris, String key, byte[] salt) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException {
        for(Uri cur_uri : uris){
            File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");
            File cur_file = new File(cur_uri.getPath());
            if(cur_file.exists()){
                Log.e("SecureGallery", cur_file.getName() + " exists");
            }else{
                Log.e("SecureGallery", cur_file.getName() + " Doesn't exist");
            }
            String fileName = getFileNameFromURI(context, cur_uri);
            Log.e("SecureGallery", "Importing filename: " + fileName);
            Log.e("SecureGallery", cur_uri.getPath());

            MimeTypeMap mime = MimeTypeMap.getSingleton();
            Log.e("SecureGallery", "TYPE: " + context.getContentResolver().getType(cur_uri));
            String ext = mime.getExtensionFromMimeType(context.getContentResolver().getType(cur_uri));
            Log.e("SecureGallery", "Determined extension: " + ext);
            File dest_file;
            if(fileName != null) {
                dest_file = new File(secure_path + File.separator + fileName);

                // If file already exists generate a unique name for it
                if(dest_file.exists()){
                    dest_file = incrementFileName(dest_file, ext);
                }
            }else{
                dest_file = new File(secure_path + File.separator + genUniqueName(ext));
                Log.e("SecureGallery", "Couldn't find name, generated: \"" + dest_file.getName() + "\"");
            }

            Log.e("SecureGallery", "Encrypting " + cur_file.getName());
            encryptNewFile(context, cur_uri, dest_file, key, salt);
        }
    }

    public static File incrementFileName(File file, String ext){
        if(!file.exists())
            return file;

        int count = 2;
        File new_file;
        do{
            String path = file.getParent();

            String nameNoExt = file.getName();
            int dotIndex = nameNoExt.lastIndexOf(".");
            if(dotIndex != -1){ // there is an extension
                nameNoExt = nameNoExt.substring(0, dotIndex);
            }

            new_file = new File(path + File.separator + nameNoExt + "_" + count + "." + ext);
        }while(new_file.exists());

        return new_file;
    }

    public static String genUniqueName(String ext){
        Random rand = new Random();

        byte[] randBytes = new byte[16];
        rand.nextBytes(randBytes);

        String name;
        do {
            name = UUID.randomUUID().toString().substring(0, 15).replace("-", "") + "." + ext;
        }while(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + name).exists());

        return name;
    }

    public static String getFileNameFromURI(Context context, Uri uri){
        if(uri.getScheme().equals("content")){
            Cursor cur = context.getContentResolver().query(uri, null, null, null, null);
            if(cur != null && cur.moveToFirst()){
                String name = cur.getString(cur.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                cur.close();
                return name;
            }
        }

        return null;
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
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, KEY_ITERATIONS, 128);
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
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, KEY_ITERATIONS, 128);
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

    public static void encryptNewFile(Context context, Uri src, File dest, String pw, byte[] salt) throws NoSuchAlgorithmException, NoSuchPaddingException, IOException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException {
        // Generate key from PW
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, KEY_ITERATIONS, 128);
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

        InputStream inStream = context.getContentResolver().openInputStream(src);

        // Read in clear bytes

        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int read;
        while((read = inStream.read(data)) != -1){
            buff.write(data, 0, read);
        }
        buff.flush();
        byte[] clearBytes = buff.toByteArray();
        buff.close();

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

    public static ArrayList<Uri> writeFilesToCache(Context context, HashMap<String, Uri> sent_uris) throws IOException {
        ArrayList<Uri> cache_uris = new ArrayList<>();
        for(String key : sent_uris.keySet()) {
            Uri src = sent_uris.get(key);
            InputStream inStream = context.getContentResolver().openInputStream(src);

            // Read in clear bytes

            File cur_file = new File(context.getFilesDir(), key);

            FileOutputStream fos = new FileOutputStream(cur_file);

            byte[] data = new byte[1024];
            int read;
            while ((read = inStream.read(data)) != -1) {
                fos.write(data, 0, read);
            }
            fos.flush();
            fos.close();

            Log.e("SecureGallery", "packagename: " + context.getPackageName());
            Uri new_uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", cur_file);
            cache_uris.add(new_uri);

            Log.e("SecureGallery", "Wrote " + key + " to cache.");
        }

        return cache_uris;
    }

    public static boolean clearImageCache(Context context){
        boolean success = true;

        File[] files = context.getFilesDir().listFiles();
        for(File file : files){
            if(!file.delete()){
                success = false;
            }
        }
        Log.e("SecureGallery", "Files: " + files.length);

        return success;
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
