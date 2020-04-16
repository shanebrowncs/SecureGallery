package ca.shanebrown.securegallery.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

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
    // Number of iterations to perform when generating key, currently limited for performance reasons
    public static final int KEY_ITERATIONS = 10000;

    /**
     * Copys and encrypts list of file URIs into secure image store
     * @param context Context for getting content resolver
     * @param uris List of URIs to add to secure store
     * @param key Decryption phrase
     * @param salt Decryption salt
     */
    public static void secureImagesByURIs(Context context, ArrayList<Uri> uris, String key, byte[] salt) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException {
        for(Uri cur_uri : uris){
            File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");
            File cur_file = new File(cur_uri.getPath());
            if(cur_file.exists()){
                Log.i("SecureGallery", cur_file.getName() + " exists");
            }else{
                Log.i("SecureGallery", cur_file.getName() + " Doesn't exist");
            }

            // Attempt to retrieve file name from URI
            String fileName = getFileNameFromURI(context, cur_uri);
            Log.i("SecureGallery", "Found file name: " + fileName);

            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String ext = mime.getExtensionFromMimeType(context.getContentResolver().getType(cur_uri));

            // Create destination file
            File dest_file;
            if(fileName != null) {
                dest_file = new File(secure_path + File.separator + fileName);

                // If file already exists generate a unique name for it
                if(dest_file.exists()){
                    dest_file = incrementFileName(dest_file, ext);
                }
            }else{
                dest_file = new File(secure_path + File.separator + genUniqueName(ext));
                Log.w("SecureGallery", "Couldn't find name, generated: \"" + dest_file.getName() + "\"");
            }

            // Encrypt file
            Log.i("SecureGallery", "Encrypting " + cur_file.getName());
            encryptNewFile(context, cur_uri, dest_file, key, salt);
        }
    }

    /**
     * Decrypts and reads in all images from secure image store
     * @param key Decryption phrase
     * @param salt Decryption salt
     * @return Array of Image/Path bundles containing decrypted images
     */
    public static ImagePathBundle[] getAllSecureImageBitmaps(String key, byte[] salt) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        String[] images = getAllSecureImagePaths();
        ImagePathBundle[] imageBundle = new ImagePathBundle[images.length];

        for(int i = 0; i < images.length; i++){
            Bitmap bmap = decryptImage(new File(images[i]), key, salt);
            imageBundle[i] = new ImagePathBundle(images[i], bmap);
        }

        return imageBundle;
    }

    /**
     * Gets list of all files in secure image store
     * @return List of absolute file paths
     */
    public static String[] getAllSecureImagePaths() throws IOException {
        File secure_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "SecureGallery");

        // Create image store and place .nomedia
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
        if(files == null) {
            throw new IOException("Files list returned null");
        }

        String[] paths = new String[files.length];
        for(int i = 0; i < paths.length; i++){
            paths[i] = files[i].getAbsolutePath();
        }

        return paths;
    }

    /**
     * Encrypts a given cleartext file and stores in "dest" location
     * @param context Context to get content resolver stream
     * @param src File to encrypt
     * @param dest Destination for encrypted file
     * @param pw Decryption phrase
     * @param salt Decryption salt
     */
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

    /**
     * Saves decrypted image to user's "Pictures" directory
     * @param file File to decrypt and save
     * @param pw Decryption phrase
     * @param salt Decryption salt
     */
    public static void saveDecryptedImage(File file, String pw, byte[] salt) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, IOException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        // Generate key from pw/salt
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, KEY_ITERATIONS, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Read in encrypted data
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        HashMap<String, byte[]> map = (HashMap<String, byte[]>)ois.readObject();
        ois.close();
        fis.close();

        byte[] iv = map.get("iv");
        byte[] cipherBytes = map.get("cipherBytes");

        // Decryption of read data
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] clearBytes = cipher.doFinal(cipherBytes);

        // Write cleartext to "Pictures" directory
        File export_path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + file.getName());

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(export_path));
        bos.write(clearBytes);
        bos.flush();
        bos.close();
    }

    /**
     * Decrypts image file and returns bitmap memory object
     * @param file Image file to decrypt/import
     * @param pw Decryption phrase
     * @param salt Decryption salt
     * @return Image bitmap memory object
     */
    public static Bitmap decryptImage(File file, String pw, byte[] salt) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {
        // Generate key from pw/salt
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, KEY_ITERATIONS, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        SecretKey key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // Read in encrypted data
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        HashMap<String, byte[]> map = (HashMap<String, byte[]>)ois.readObject();
        ois.close();
        fis.close();

        byte[] iv = map.get("iv");
        byte[] cipherBytes = map.get("cipherBytes");

        // Decryption of read data
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        // Convert to bitmap
        byte[] clearBytes = cipher.doFinal(cipherBytes);
        return BitmapFactory.decodeByteArray(clearBytes, 0, clearBytes.length);
    }

    /**
     * Finds a unique file name from orignal name and number suffix
     * @param file File to change name of
     * @param ext File extension of given file
     * @return New file with unique name
     */
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

    /**
     * Generate a unique file name from a UUID
     * @param ext Extension of file
     * @return Unique file name String with proper extension
     */
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

    /**
     * Attempts to retrieve the file name from a URI
     * @param context Context for getting content resovler
     * @param uri URI to find file name for
     * @return String containing file's name or null
     */
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

    /**
     * Writes files in cleartext to temporary cache, used to get around limited content provider scope
     * @param context Context to get content resolver
     * @param sent_uris URIs to copy to cache
     * @return List of URIs of files in our cache
     */
    public static ArrayList<Uri> writeFilesToCache(Context context, HashMap<String, Uri> sent_uris) throws IOException {
        ArrayList<Uri> cache_uris = new ArrayList<>();
        for(String key : sent_uris.keySet()) {
            Uri src = sent_uris.get(key);
            if(src == null){
                Log.w("SecureGallery", "Found null src when writing to cache, this probably shouldn't happen");
                continue;
            }

            // Read in clear bytes
            InputStream inStream = context.getContentResolver().openInputStream(src);
            File cur_file = new File(context.getFilesDir(), key);

            // Output clear bytes to cache
            FileOutputStream fos = new FileOutputStream(cur_file);
            byte[] data = new byte[1024];
            int read;
            while ((read = inStream.read(data)) != -1) {
                fos.write(data, 0, read);
            }
            fos.flush();
            fos.close();

            // Add freshly written clear image to URI list
            Uri new_uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", cur_file);
            cache_uris.add(new_uri);

            Log.i("SecureGallery", "Wrote " + key + " to cache.");
        }

        return cache_uris;
    }

    /**
     * Empties cache of all images
     * @param context Context to get internal app storage
     * @return True on success, false on failure
     */
    public static boolean clearImageCache(Context context){
        boolean success = true;

        File[] files = context.getFilesDir().listFiles();
        if(files == null)
            return false;

        for(File file : files){
            if(!file.delete()){
                success = false;
            }
        }

        return success;
    }

    /**
     * Copies src file to dest
     * @param src File to copy
     * @param dest Destination for copy
     * @return True on sucesss, false on failure
     */
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
                Log.w("SecureGallery", "IOException while writing dest file");
                return false;
            }
        }catch(FileNotFoundException ex){
            Log.w("SecureGallery", "File to copy not found");
            return false;
        }

        return true;
    }

    /**
     * Deletes given file from fileystem
     * @param file File to delete
     * @return True on success, false on failure
     */
    public static boolean deleteFile(File file){
        if(file.exists()){
            return file.delete();
        }

        return false;
    }

    /**
     * Request external storage write permissions for activity
     * @param act Activity for request
     * @return Returns true if permission is already granted, false if permission needed to be requested
     */
    public static boolean requestWritePermissions(Activity act){
        if(Build.VERSION.SDK_INT >= 23) {
            if (act.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE} ,1);
                return false; // Return false if we didn't have the permission and we had to request it.
            }
        }else{
            Log.i("SecureGallery", "SDK < 23, requesting permissions not needed");
        }

        return true;
    }
}
