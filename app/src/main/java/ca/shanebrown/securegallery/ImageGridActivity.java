package ca.shanebrown.securegallery;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class ImageGridActivity extends AppCompatActivity implements ImageGridRecyclerAdapter.OnImageClickListener {

    private String[] images;

    private ImageGridRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);

        RecyclerView recyclerView = findViewById(R.id.grid_recycler);
        NoMarginDecoration dec = new NoMarginDecoration(0);
        recyclerView.addItemDecoration(dec);

        Intent thisIntent = getIntent();
        ArrayList<Uri> sent_uris = thisIntent.getParcelableArrayListExtra("sent_uris");

        if(sent_uris != null && !sent_uris.isEmpty()){
            handleSentItems(sent_uris);
        }

        images = new String[]{};
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

        adapter = new ImageGridRecyclerAdapter(images, this);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
    }

    @Override
    public void onImageClick(int position) {
        Log.e("SecureGallery", "Activity pos: " + position);
        if(position < images.length){
            Intent intent = new Intent(this, ImageViewActivity.class);
            intent.putExtra("image", images[position]);

            startActivity(intent);
        }else{
            Toast.makeText(this, "Clicked an image that we don't have a reference to, this shouldn't happen.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleSentItems(final ArrayList<Uri> sent_uris){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(ImageGridActivity.this, "Transferring and securing images..", Toast.LENGTH_LONG).show();
                ImageHandler.secureImagesByURIs(sent_uris);
                try {
                    String[] new_images = ImageHandler.getAllSecureImages();
                    images = new_images;
                    adapter.refreshImages(images);
                }catch(IOException ex){
                    Toast.makeText(ImageGridActivity.this, "Failed fetching images from secure gallery", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(ImageGridActivity.this, "Cancelling securing sent items", Toast.LENGTH_LONG).show();
            }
        });

        builder.setTitle("Are you sure?");
        builder.setMessage("Are you sure you would like to add these " + sent_uris.size() + " items to SecureGallery? They will be moved out of their current directories and encrypted.");

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}