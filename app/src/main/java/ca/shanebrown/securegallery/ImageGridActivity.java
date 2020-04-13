package ca.shanebrown.securegallery;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class ImageGridActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);

        RecyclerView recyclerView = findViewById(R.id.grid_recycler);


        String[] images = new String[]{};
        try {
            images = ImageHandler.getAllSecureImages();
        }catch(IOException ex){
            Toast.makeText(this, "Insufficient storage permissions", Toast.LENGTH_LONG).show();
            boolean status = ImageHandler.requestWritePermissions(this);
            Log.e("SecureGallery", "Return status: " + String.valueOf(status));
            return;
        }

        if(images.length < 1){
            Log.e("SecureGallery", "No images");
        }

        for(String image : images){
            Log.e("SecureGallery", image);
        }

        ImageGridRecyclerAdapter adapter = new ImageGridRecyclerAdapter(images);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }
}