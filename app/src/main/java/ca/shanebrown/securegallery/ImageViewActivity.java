package ca.shanebrown.securegallery;

import android.app.ActionBar;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ImageViewActivity extends AppCompatActivity {


    String imagePath = null;
    String key = null;
    byte[] salt = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        // Back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent thisIntent = getIntent();
        imagePath = thisIntent.getStringExtra("image");
        key = thisIntent.getStringExtra("key");
        salt = thisIntent.getByteArrayExtra("salt");

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

        // Set activity title
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

    private void deleteImageUserAction(){
        // Ask user first
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ImageHandler.deleteFile(new File(imagePath));
                Toast.makeText(ImageViewActivity.this, "Image deleted.", Toast.LENGTH_SHORT).show();
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
