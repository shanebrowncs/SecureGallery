package ca.shanebrown.securegallery.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import ca.shanebrown.securegallery.R;
import ca.shanebrown.securegallery.fragments.InformationFragment;

public class IntroductionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction_activity);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.introduction_frame_layout, new InformationFragment());
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        FragmentManager manager = getSupportFragmentManager();
        if(manager.getBackStackEntryCount() > 0){
            manager.popBackStack();
        }else{
            Log.w("SecureGallery", "Cannot find IntroductionFragment to pop back to");
            super.onBackPressed();
        }
    }
}