package ca.shanebrown.securegallery.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.IOException;

import ca.shanebrown.securegallery.util.ImageHandler;
import ca.shanebrown.securegallery.R;

public class ImageViewActivity extends AppCompatActivity {

    private String imagePath = null;
    private String key = null;
    private byte[] salt = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        // Set back button on actionbar
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Retrieve decryption and image information
        Intent thisIntent = getIntent();
        imagePath = thisIntent.getStringExtra("image");
        key = thisIntent.getStringExtra("key");
        salt = thisIntent.getByteArrayExtra("salt");

        // Decrypt image
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

        // Display image
        PhotoView photoView = findViewById(R.id.photo_view);
        if(image != null){
            photoView.setImageBitmap(image);
        }

        setTitle(new File(imagePath).getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_view_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.action_image_view_delete:
                deleteImageUserAction();
                break;
            case R.id.action_image_view_export:
                exportImageUserAction();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    /**
     * Handles user action of deleting currently displayed image from secure store
     */
    private void deleteImageUserAction(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ImageHandler.deleteFile(new File(imagePath));
                Toast.makeText(ImageViewActivity.this, "Image deleted.", Toast.LENGTH_SHORT).show();

                // Send result to ImageGridActivity to trigger recycler refresh
                Intent returnIntent = new Intent();
                returnIntent.putExtra("changed", true);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setTitle("Are you sure?");
        builder.setMessage("Are you sure you would like to delete this image? It will not be recoverable.");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Handles user action for exporting current image in decrypted form
     */
    private void exportImageUserAction(){
        try {
            ImageHandler.saveDecryptedImage(new File(imagePath), key, salt);
            Toast.makeText(this, "Images saved to Pictures directory", Toast.LENGTH_SHORT).show();
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "Image corruption, cannot recognize format.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error reading data from file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            Toast.makeText(this, "Crypto exception, likely a needed algorithm is not supported by the device", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
