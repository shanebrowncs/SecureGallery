package ca.shanebrown.securegallery;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageGridActivity extends AppCompatActivity implements ImageGridRecyclerAdapter.OnImageClickListener {

    private String key;
    private byte[] salt;
    private ImagePathBundle[] images;
    private ImageGridRecyclerAdapter adapter;

    private ActionMode selectionMode;
    private SelectionModeCallback selectionModeCallback = new SelectionModeCallback();

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
        this.salt = salt;

        images = new ImagePathBundle[]{};
        try {
            images = ImageHandler.getAllSecureImageBitmaps(key, salt);
        }catch(IOException ex){
            ex.printStackTrace();
            Toast.makeText(this, "Insufficient storage permissions", Toast.LENGTH_LONG).show();
            boolean status = ImageHandler.requestWritePermissions(this);
            Log.e("SecureGallery", "Return status: " + String.valueOf(status));
            return;
        }catch(Exception ex){
            ex.printStackTrace();
            Toast.makeText(this, "Image decryption failed, " + ex.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            return;
        }

        if(images.length < 1){
            Log.e("SecureGallery", "No images");
        }

        adapter = new ImageGridRecyclerAdapter(images, ImageGridActivity.this, this);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
    }

    @Override
    public void onImageClick(int position) {
        if(selectionMode != null){
            toggleItemSelection(position);
        }else {
            if (position < images.length) {
                Intent intent = new Intent(this, ImageViewActivity.class);
                intent.putExtra("image", images[position].getPath());
                intent.putExtra("key", key);
                intent.putExtra("salt", salt);

                startActivity(intent);
            } else {
                Toast.makeText(this, "Clicked an image that we don't have a reference to, this shouldn't happen.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onImageLongClick(int position) {
        if(selectionMode == null){
            selectionMode = startSupportActionMode(selectionModeCallback);
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
                    ImagePathBundle[] bmaps = ImageHandler.getAllSecureImageBitmaps(key, salt);
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

    private void toggleItemSelection(int position){
        adapter.toggleSelection(position);

        int count = adapter.getSelectedItemCount();
        if(count == 0){
            selectionMode.finish();
        }else{
            selectionMode.setTitle(String.valueOf(count));
            selectionMode.invalidate();
        }
    }

    private boolean deleteSelectedImages(List<Integer> items){
        // Delete selected items
        boolean success = true;
        for(Integer item : items){
            if(!ImageHandler.deleteFile(new File(images[item].getPath()))){
                success = false;
            }
        }

        // Refresh item list after deletion
        try {
            images = ImageHandler.getAllSecureImageBitmaps(key, salt);
            this.images = images;
            adapter.refreshImages(images, false);
        }catch(IOException ex){
            Toast.makeText(ImageGridActivity.this, "Failed fetching images from secure gallery", Toast.LENGTH_LONG).show();
        }catch(Exception ex){
            Toast.makeText(ImageGridActivity.this, "Failed fetching images from secure gallery, crypto exception", Toast.LENGTH_LONG).show();
        }

        // Notify adapter of which items were deleted
        for(Integer item : items){
            adapter.notifyItemRemoved(item);
        }

        return success;
    }

    private void saveDecryptedImageSelectedUserAction(){
        List<Integer> items = adapter.getSelectedItems();

        boolean success = true;

        for(Integer item : items){
            try {
                ImageHandler.saveDecryptedImage(new File(images[item].getPath()), key, salt);
            }catch(IOException ex){
                Toast.makeText(ImageGridActivity.this, "IOException while writing file " + new File(images[item].getPath()).getName(), Toast.LENGTH_LONG).show();
                success = false;
            }catch(Exception ex){
                Toast.makeText(ImageGridActivity.this, "Crypto error, couldn't decrypt image " + new File(images[item].getPath()).getName(), Toast.LENGTH_LONG).show();
                success = false;
            }
        }

        Toast.makeText(ImageGridActivity.this, "Images saved to Pictures directory", Toast.LENGTH_LONG).show();
    }

    private void deleteSelectedUserAction(){
        int itemCount = adapter.getSelectedItemCount();
        final List<Integer> items = adapter.getSelectedItems();

        // Ask user first
        AlertDialog.Builder builder = new AlertDialog.Builder(ImageGridActivity.this);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteSelectedImages(items);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setTitle("Are you sure?");
        builder.setMessage("Are you sure you would like to delete these " + itemCount + " images? They will not be recoverable.");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private class SelectionModeCallback implements androidx.appcompat.view.ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.selection_mode_options, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(androidx.appcompat.view.ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(androidx.appcompat.view.ActionMode mode, MenuItem item) {
            switch(item.getItemId()){
                case R.id.menu_selection_delete:
                    deleteSelectedUserAction();
                    mode.finish();
                    break;
                case R.id.menu_selection_export_decrypted:
                    saveDecryptedImageSelectedUserAction();
                    mode.finish();
                    break;
                default:
                    return false;
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(androidx.appcompat.view.ActionMode mode) {
            Log.e("SecureGallery", "Action Destroy Called");
            adapter.clearSelection();
            selectionMode = null;
        }
    }
}