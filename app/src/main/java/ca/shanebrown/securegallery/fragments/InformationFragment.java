package ca.shanebrown.securegallery.fragments;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import ca.shanebrown.securegallery.R;

public class InformationFragment extends Fragment {

    public InformationFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_information, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button nextBtn = view.findViewById(R.id.infoFragNextBtn);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start "NewPasswordFragment"
                Activity parentAct = getActivity();
                if(parentAct != null) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.setCustomAnimations(R.anim.custom_slide_left_in, R.anim.custom_slide_left_out, R.anim.custom_slide_right_in, R.anim.custom_slide_right_out);
                    ft.replace(R.id.introduction_frame_layout, new NewPasswordFragment());
                    ft.addToBackStack(null);
                    ft.commit();
                }
            }
        });
    }
}
