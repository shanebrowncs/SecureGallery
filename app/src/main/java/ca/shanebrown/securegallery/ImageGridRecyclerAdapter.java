package ca.shanebrown.securegallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ImageGridRecyclerAdapter extends RecyclerView.Adapter<ImageGridRecyclerAdapter.ImageGridRecyclerViewHolder> {
    String[] images;

    public ImageGridRecyclerAdapter(String[] images) {
        this.images = images;
    }

    @NonNull
    @Override
    public ImageGridRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View customView = inflater.inflate(R.layout.grid_recycler_item, parent, false);

        ImageGridRecyclerViewHolder vh = new ImageGridRecyclerViewHolder(customView);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageGridRecyclerViewHolder holder, int position) {
        Bitmap bmap = BitmapFactory.decodeFile(this.images[position]);
        holder.imageView.setImageBitmap(bmap);
        holder.imageView.invalidate();
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    public static class ImageGridRecyclerViewHolder extends RecyclerView.ViewHolder{
        public ImageView imageView;
        public ImageGridRecyclerViewHolder(View v) {
            super(v);
            imageView = v.findViewById(R.id.grid_image_view);
        }
    }
}
