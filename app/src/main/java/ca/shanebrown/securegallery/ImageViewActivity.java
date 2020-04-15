package ca.shanebrown.securegallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.IOException;

public class ImageViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        Intent thisIntent = getIntent();
        String imagePath = thisIntent.getStringExtra("image");
        String key = thisIntent.getStringExtra("key");
        byte[] salt = thisIntent.getByteArrayExtra("salt");

        Bitmap image = null;
        try {
            image = ImageHandler.decryptImage(new File(imagePath), key, salt);
        } catch (IOException e) {
            Toast.makeText(this, "Fatal: Failed to read file", Toast.LENGTH_LONG).show();
            finish();
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "Fatal: Corrupted file", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Fatal: Failed to decrypt file", Toast.LENGTH_LONG).show();
            finish();
        }

        PhotoView photoView = findViewById(R.id.photo_view);

        if(image != null){
            photoView.setImageBitmap(image);
        }
    }
}
