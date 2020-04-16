package ca.shanebrown.securegallery.util;

import android.graphics.Bitmap;

public class ImagePathBundle {
    private String path;
    private Bitmap image;

    public ImagePathBundle(String path, Bitmap image){
        this.path = path;
        this.image = image;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        if(path != null && !path.isEmpty()) {
            this.path = path;
        }
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        if(image != null) {
            this.image = image;
        }
    }
}
