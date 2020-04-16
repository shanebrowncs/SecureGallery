package ca.shanebrown.securegallery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.andrognito.patternlockview.utils.ResourceUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewPasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewPasswordFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private String password = null;

    public NewPasswordFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NewPasswordFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NewPasswordFragment newInstance(String param1, String param2) {
        NewPasswordFragment fragment = new NewPasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
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
        mPatternLockView.setCorrectStateColor(ResourceUtils.getColor(getActivity(), R.color.white));
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

                mPatternLockView.clearPattern();

                // First time entering
                if(password == null){
                    Log.e("SecureGallery", "First time");
                    password = entered_pw;
                    enterText.setText("Confirm Password");
                }else{
                    Log.e("SecureGallery", "Confirm");
                    Log.e("SecureGallery", password + " == " + entered_pw);
                    if(entered_pw.equals(password)){
                        if(savePassword(password)) {
                            Toast.makeText(getActivity(), "Password created", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                    }else{
                        Toast.makeText(getActivity(), "Passwords do not match", Toast.LENGTH_LONG).show();
                        password = null;
                        enterText.setText("Enter New Password");
                    }
                }
            }

            @Override
            public void onCleared() {

            }
        });
    }



    public boolean savePassword(String pw){
        SecretKeyFactory factory = null;
        try{
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); // Apparently this is a good choice for android
        }catch(NoSuchAlgorithmException ex){
            Toast.makeText(getActivity(), "Fatal: OS does not support PBKDF2 hashing", Toast.LENGTH_LONG).show();
            return false;
        }

        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[16];
        random.nextBytes(salt);

        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, ImageHandler.KEY_ITERATIONS, 128);

        byte[] hash = null;
        try {
            hash = factory.generateSecret(spec).getEncoded();
        }catch(InvalidKeySpecException ex){
            Toast.makeText(getActivity(), "Fatal: PW hash generation failed", Toast.LENGTH_LONG).show();
            return false;
        }

        try {
            Log.e("SecureGallery", "NEW hash: " + new String(hash, "UTF-8") + ", salt: " + new String(salt, "UTF-8"));
        }catch(Exception ex){

        }

        SecureDatabase db = new SecureDatabase(getActivity());

        if(!db.savePassword(hash, salt)){
            Toast.makeText(getActivity(), "Fatal: Committing PW to database failed", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }
}
