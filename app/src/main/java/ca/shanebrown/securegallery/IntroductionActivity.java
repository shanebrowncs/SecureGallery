package ca.shanebrown.securegallery;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;

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
            Log.e("SecureGallery", "pop to previous fragment");
            manager.popBackStack();
        }else{
            Log.e("SecureGallery", "No previous fragment");
            super.onBackPressed();
        }
    }
}