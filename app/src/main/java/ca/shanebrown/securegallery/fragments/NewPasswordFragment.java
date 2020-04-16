package ca.shanebrown.securegallery.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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

import ca.shanebrown.securegallery.R;
import ca.shanebrown.securegallery.activities.MainActivity;
import ca.shanebrown.securegallery.util.ImageHandler;
import ca.shanebrown.securegallery.util.SecureDatabase;

public class NewPasswordFragment extends Fragment {
    private String password = null;

    public NewPasswordFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        Activity parentAct = getActivity();

        // Ensure we can find activity
        if(parentAct == null) {
            Log.w("SecureGallery", "Can't find main activity in NewPasswordFragment");
            Toast.makeText(view.getContext(), "Can't find main activity, closing.", Toast.LENGTH_SHORT).show();

            FragmentManager fm = getFragmentManager();
            if (fm != null){
                fm.beginTransaction().remove(NewPasswordFragment.this).commit();
            }else{
                Log.e("SecureGallery", "Can't find activity or fragment manager, nothing to do.");
                return;
            }
        }

        // Initialize settings for pattern lock
        final PatternLockView mPatternLockView = view.findViewById(R.id.new_pw_pattern_lock);
        mPatternLockView.setDotCount(6);
        mPatternLockView.setDotNormalSize((int) ResourceUtils.getDimensionInPx(getActivity(), R.dimen.pattern_lock_dot_size));
        mPatternLockView.setDotSelectedSize((int) ResourceUtils.getDimensionInPx(getActivity(), R.dimen.pattern_lock_dot_selected_size));
        mPatternLockView.setPathWidth((int) ResourceUtils.getDimensionInPx(getActivity(), R.dimen.pattern_lock_path_width));
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
                TextView enterText = view.findViewById(R.id.passwordHintLbl);
                String entered_pw = PatternLockUtils.patternToString(mPatternLockView, pattern);

                // First time entering
                if(password == null){
                    mPatternLockView.clearPattern();
                    password = entered_pw;
                    enterText.setText(R.string.confirm_password);
                }else{
                    // Clear pattern after 3 seconds
                    mPatternLockView.setInputEnabled(false);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mPatternLockView.clearPattern();
                            mPatternLockView.setInputEnabled(true);
                        }
                    }, 2000);

                    // Check password confirmation matches
                    if(entered_pw.equals(password)){
                        mPatternLockView.setViewMode(PatternLockView.PatternViewMode.CORRECT);
                        if(savePassword(password)) {
                            Toast.makeText(getActivity(), "Password created", Toast.LENGTH_LONG).show();

                            // Password added to db, now take user to home screen
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                            Activity parentAct = getActivity();

                            if(parentAct != null) {
                                getActivity().finish();
                            }else{
                                Log.e("SecureGallery", "Null activity, cannot close intro activity");
                            }
                        }
                    }else{
                        mPatternLockView.setViewMode(PatternLockView.PatternViewMode.WRONG);
                        Toast.makeText(getActivity(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                        password = null;
                        enterText.setText(R.string.enter_new_password);
                    }
                }
            }

            @Override
            public void onCleared() {

            }
        });
    }

    /**
     * Saves password derived key in sqlite database
     * @param pw Passphrase String given by user
     * @return True on success, false on failure
     */
    private boolean savePassword(String pw){
        SecretKeyFactory factory;
        try{
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); // Apparently this is a good choice for android
        }catch(NoSuchAlgorithmException ex){
            Toast.makeText(getActivity(), "Fatal: OS does not support PBKDF2 hashing", Toast.LENGTH_LONG).show();
            return false;
        }

        // Generate salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        // Generate key and hash
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, ImageHandler.KEY_ITERATIONS, 128);
        byte[] hash;
        try {
            hash = factory.generateSecret(spec).getEncoded();
        }catch(InvalidKeySpecException ex){
            Toast.makeText(getActivity(), "Fatal: PW hash generation failed", Toast.LENGTH_LONG).show();
            return false;
        }

        // Save key and salt to db
        SecureDatabase db = new SecureDatabase(getActivity());
        if(!db.savePassword(hash, salt)){
            Toast.makeText(getActivity(), "Fatal: Committing PW to database failed", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}
