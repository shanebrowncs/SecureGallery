package ca.shanebrown.securegallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ImageGridRecyclerAdapter extends RecyclerView.Adapter<ImageGridRecyclerAdapter.ImageGridRecyclerViewHolder> {
    Bitmap[] images;

    private OnImageClickListener listener;

    public ImageGridRecyclerAdapter(Bitmap[] images, OnImageClickListener listener) {
        this.images = images;
        this.listener = listener;
    }

    public void refreshImages(Bitmap[] images){
        this.images = images;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageGridRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View customView = inflater.inflate(R.layout.grid_recycler_item, parent, false);

        ImageGridRecyclerViewHolder vh = new ImageGridRecyclerViewHolder(customView, listener);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageGridRecyclerViewHolder holder, int position) {
        holder.imageView.setImageBitmap(this.images[position]);
        holder.imageView.invalidate();
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    public static class ImageGridRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView imageView;
        OnImageClickListener listener;
        public ImageGridRecyclerViewHolder(View v, OnImageClickListener listener) {
            super(v);
            imageView = v.findViewById(R.id.grid_image_view);

            this.listener = listener;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.e("SecureGallery", "pos: " + getAdapterPosition());
            listener.onImageClick(getAdapterPosition());
        }
    }

    public interface OnImageClickListener{
        void onImageClick(int position);
    }
}