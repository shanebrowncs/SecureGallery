package ca.shanebrown.securegallery;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

public class ImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        Intent thisIntent = getIntent();
        String imagePath = thisIntent.getStringExtra("image");

        PhotoView photoView = findViewById(R.id.photo_view);

        if(imagePath != null && !imagePath.isEmpty()){
            photoView.setImageURI(Uri.fromFile(new File(imagePath)));
        }
    }
}
