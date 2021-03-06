package ca.shanebrown.securegallery.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.andrognito.patternlockview.utils.ResourceUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import ca.shanebrown.securegallery.util.ImageHandler;
import ca.shanebrown.securegallery.R;
import ca.shanebrown.securegallery.util.SecureDatabase;

public class MainActivity extends AppCompatActivity {

    private final HashMap<String, Uri> sent_uris = new HashMap<>();
    private ArrayList<Uri> filesToImport = null;
    private String key = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final PatternLockView mPatternLockView = findViewById(R.id.pattern_lock);

        // If no password present in keystore start intro activity
        SecureDatabase db = new SecureDatabase(this);
        ContentValues values = db.getPassword();

        if(values == null){
            Intent intent = new Intent(this, IntroductionActivity.class);
            startActivity(intent);
            finish();
        }else{
            mPatternLockView.setVisibility(View.VISIBLE);
        }

        // Handle accepting of images
        Intent thisIntent = getIntent();
        String action = thisIntent.getAction();
        String type = thisIntent.getType();

        if(Intent.ACTION_SEND.equals(action) && type != null){
            Log.i("SecureGallery", "ACTION_SEND set");
            if(type.startsWith("image/")){ // Handle single image
                Uri single_uri = thisIntent.getParcelableExtra(Intent.EXTRA_STREAM);

                if(single_uri != null) {
                    String imageName = ImageHandler.getFileNameFromURI(this, single_uri);
                    assert imageName != null;
                    sent_uris.put(imageName, single_uri);
                }
            }
        }else if(Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null){
            Log.i("SecureGallery", "ACTION_SEND_MULTIPLE set");
            if(type.startsWith("image/")) {
                ArrayList<Uri> URIs = thisIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if(URIs != null) {
                    for (Uri uri : URIs) {
                        String imageName = ImageHandler.getFileNameFromURI(this, uri);
                        assert imageName != null;
                        sent_uris.put(imageName, uri);
                    }
                }else{
                    Log.w("SecureGallery", "ACTION_SEND_MUTLIPLE URI list extra null?");
                    Toast.makeText(this, "Sending app did not properly bundle images", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Write given images to cache for accessing from ImageGridActivity
        if(sent_uris.size() > 0){
            try {
                filesToImport = ImageHandler.writeFilesToCache(this, sent_uris);
            } catch (IOException e) {
                e.printStackTrace();
                Log.w("SecureGallery", "Exception writing files to cache");
            }
        }

        // Initialize settings for pattern lock
        mPatternLockView.setDotCount(6);
        mPatternLockView.setDotNormalSize((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_dot_size));
        mPatternLockView.setDotSelectedSize((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_dot_selected_size));
        mPatternLockView.setPathWidth((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_path_width));
        mPatternLockView.setAspectRatioEnabled(true);
        mPatternLockView.setAspectRatio(PatternLockView.AspectRatio.ASPECT_RATIO_HEIGHT_BIAS);
        mPatternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT);
        mPatternLockView.setDotAnimationDuration(150);
        mPatternLockView.setPathEndAnimationDuration(100);
        mPatternLockView.setInStealthMode(false);
        mPatternLockView.setTactileFeedbackEnabled(true);
        mPatternLockView.setInputEnabled(true);
        mPatternLockView.addPatternLockListener(new PatternLockViewListener() {
            @Override
            public void onStarted() {

            }

            @Override
            public void onProgress(List<PatternLockView.Dot> progressPattern) {

            }

            @Override
            public void onComplete(List<PatternLockView.Dot> pattern) {
                String entered_pw = PatternLockUtils.patternToString(mPatternLockView, pattern);
                mPatternLockView.setInputEnabled(false);

                // Clear pattern after 3 seconds
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPatternLockView.clearPattern();
                        mPatternLockView.setInputEnabled(true);
                    }
                }, 2000);

                if(verifyPassword(entered_pw)){
                    key = entered_pw;
                    mPatternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT);

                    // Verify we have external storage permissions
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                            return;
                        }
                    }

                    // We have permission or build is < M
                    startAppMainPage(entered_pw);
                }else{
                    mPatternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG);
                }
            }

            @Override
            public void onCleared() {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.i("SecureGallery", "Permission gained, can now let through");
            startAppMainPage(key);
        }else{
            Log.i("SecureGallery", "User denied external storage permissions");
            Toast.makeText(this, "You must allow external storage permissions to continue.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Setup and starting of ImageGridActivity
     * @param key Decryption phrase
     */
    private void startAppMainPage(@NonNull String key){
        Intent intent = new Intent(MainActivity.this, ImageGridActivity.class);
        intent.putExtra("filesToImport", filesToImport);
        intent.putExtra("key", key);

        startActivity(intent);
        finish();
    }

    /**
     * Verifies if given password is correct, comparing with database key
     * @param entered_pw Entered password String
     * @return True on correct, False on incorrect
     */
    private boolean verifyPassword(String entered_pw){
        SecureDatabase db = new SecureDatabase(this);
        ContentValues values = db.getPassword();

        // Generate key from phrase
        SecretKeyFactory factory;
        try{
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); // Apparently this is a good choice for android
        }catch(NoSuchAlgorithmException ex){
            Toast.makeText(this, "Fatal: OS does not support PBKDF2 hashing", Toast.LENGTH_LONG).show();
            return false;
        }
        KeySpec spec = new PBEKeySpec(entered_pw.toCharArray(), values.getAsByteArray("salt"), ImageHandler.KEY_ITERATIONS, 128);

        // Generate hash from key
        byte[] hash;
        try {
            hash = factory.generateSecret(spec).getEncoded();
        }catch(InvalidKeySpecException ex){
            Toast.makeText(this, "Fatal: PW hash generation failed", Toast.LENGTH_LONG).show();
            return false;
        }

        String hash64 = Base64.encodeToString(hash, Base64.DEFAULT);
        String storedHash64 = Base64.encodeToString(values.getAsByteArray("secret"), Base64.DEFAULT);

        // FINAL PASSWORD CHECK
        return hash64.equals(storedHash64);
    }
}