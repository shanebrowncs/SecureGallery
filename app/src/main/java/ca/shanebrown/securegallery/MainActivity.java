package ca.shanebrown.securegallery;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.andrognito.patternlockview.utils.ResourceUtils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final PatternLockView mPatternLockView = (PatternLockView)findViewById(R.id.pattern_lock);

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


        mPatternLockView.setDotCount(6);
        mPatternLockView.setDotNormalSize((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_dot_size));
        mPatternLockView.setDotSelectedSize((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_dot_selected_size));
        mPatternLockView.setPathWidth((int) ResourceUtils.getDimensionInPx(this, R.dimen.pattern_lock_path_width));
        mPatternLockView.setAspectRatioEnabled(true);
        mPatternLockView.setAspectRatio(PatternLockView.AspectRatio.ASPECT_RATIO_HEIGHT_BIAS);
        mPatternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT);
        mPatternLockView.setDotAnimationDuration(150);
        mPatternLockView.setPathEndAnimationDuration(100);
        mPatternLockView.setCorrectStateColor(ResourceUtils.getColor(this, R.color.white));
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
                verifyPassword(entered_pw);
                mPatternLockView.clearPattern();
            }

            @Override
            public void onCleared() {

            }
        });


    }

    public boolean verifyPassword(String entered_pw){
        SecureDatabase db = new SecureDatabase(this);
        ContentValues values = db.getPassword();

        SecretKeyFactory factory = null;
        try{
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); // Apparently this is a good choice for android
        }catch(NoSuchAlgorithmException ex){
            Toast.makeText(this, "Fatal: OS does not support PBKDF2 hashing", Toast.LENGTH_LONG).show();
            return false;
        }

        KeySpec spec = new PBEKeySpec(entered_pw.toCharArray(), values.getAsByteArray("salt"), 65536, 128);

        byte[] hash = null;
        try {
            hash = factory.generateSecret(spec).getEncoded();
        }catch(InvalidKeySpecException ex){
            Toast.makeText(this, "Fatal: PW hash generation failed", Toast.LENGTH_LONG).show();
            return false;
        }

        String hash64 = Base64.encodeToString(hash, Base64.DEFAULT);
        String storedHash64 = Base64.encodeToString(values.getAsByteArray("secret"), Base64.DEFAULT);
        String storedSalt64 = Base64.encodeToString(values.getAsByteArray("salt"), Base64.DEFAULT);

        try {
            Log.e("SecureGallery", "Entered hash: " + hash64);
            Log.e("SecureGallery", "Stored hash: " + storedHash64 + ", salt: " + storedSalt64);

        }catch(Exception ex){

        }

        // FINAL PASSWORD CHECK
        if(hash64.equals(storedHash64)){
            Toast.makeText(this, "Good password", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this, "Bad password", Toast.LENGTH_LONG).show();
        }

        return false;
    }
}