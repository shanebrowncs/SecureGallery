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
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.NoSuchPaddingException;

public class ImageGridActivity extends AppCompatActivity implements ImageGridRecyclerAdapter.OnImageClickListener {

    private String key;
    private Bitmap[] images;
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
        key = thisIntent.getStringExtra("key");

        if(key == null || key.isEmpty()){
            Toast.makeText(this, "Fatal, key was not passed to gallery activity.", Toast.LENGTH_LONG).show();
            Log.e("SecureGallery", "Gallery activity received empty key");
            finish();
        }else{
            Log.e("SecureGallery", "Given key: " + key);
        }

        if(sent_uris != null && !sent_uris.isEmpty()){
            handleSentItems(sent_uris, key);
        }

        SecureDatabase db = new SecureDatabase(this);
        byte[] salt = db.getPassword().getAsByteArray("salt");

        images = new Bitmap[]{};
        try {
            images = ImageHandler.getAllSecureImageBitmaps(key, salt);
        }catch(IOException ex){
            Toast.makeText(this, "Insufficient storage permissions", Toast.LENGTH_LONG).show();
            boolean status = ImageHandler.requestWritePermissions(this);
            Log.e("SecureGallery", "Return status: " + String.valueOf(status));
            return;
        }catch(Exception ex){
            Toast.makeText(this, "Image decryption failed, " + ex.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            return;
        }

        if(images.length < 1){
            Log.e("SecureGallery", "No images");
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

    private void handleSentItems(final ArrayList<Uri> sent_uris, final String key){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(ImageGridActivity.this, "Transferring and securing images..", Toast.LENGTH_LONG).show();

                SecureDatabase db = new SecureDatabase(ImageGridActivity.this);
                byte[] salt = db.getPassword().getAsByteArray("salt");

                // Encrypt given URIs
                try {
                    ImageHandler.secureImagesByURIs(sent_uris, key, salt);
                }catch (IOException e) {
                    Log.e("SecureGallery", "secure images crypto exception");
                    Toast.makeText(ImageGridActivity.this, "IOException when attempting encryption, disk space or invalid permissions?", Toast.LENGTH_LONG).show();
                    return;
                }catch(Exception ex){
                    Log.e("SecureGallery", "secure images crypto exception");
                    ex.printStackTrace();
                    Toast.makeText(ImageGridActivity.this, "Crypto exception occurred, likely the device does not support needed algorithm", Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    Bitmap[] bmaps = ImageHandler.getAllSecureImageBitmaps(key, salt);
                    images = bmaps;
                    adapter.refreshImages(images);
                }catch(IOException ex){
                    Toast.makeText(ImageGridActivity.this, "Failed fetching images from secure gallery", Toast.LENGTH_LONG).show();
                }catch(Exception ex){
                    Toast.makeText(ImageGridActivity.this, "Failed fetching images from secure gallery, crypto exception", Toast.LENGTH_LONG).show();
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
        builder.setMessage("Are you sure you would like to add these " + sent_uris.size() + " items to SecureGallery? They will be copied out of their current locations and encrypted. Once you've verified they can be decrypted properly you can backup and delete the originals.");

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}