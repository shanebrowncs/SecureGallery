package ca.shanebrown.securegallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImageGridRecyclerAdapter extends RecyclerView.Adapter<ImageGridRecyclerAdapter.ImageGridRecyclerViewHolder> {
    private Bitmap[] images;
    private SparseBooleanArray selected;
    private Context context;

    private OnImageClickListener listener;

    public ImageGridRecyclerAdapter(Bitmap[] images, Context context, OnImageClickListener listener) {
        this.images = images;
        this.context = context;
        this.listener = listener;


        selected = new SparseBooleanArray();
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

        if(isSelected(position)){
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                holder.imageView.setBackground(ContextCompat.getDrawable(context, R.drawable.border_selected));
            }else{
                // We gotta figure something else to do
            }
        }else{
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                holder.imageView.setBackground(ContextCompat.getDrawable(context, R.drawable.border));
            }else{
                // We gotta figure something else to do
            }
        }
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    // SELECTION CODE
    public List<Integer> getSelectedItems(){
        List<Integer> items = new ArrayList<>(selected.size());
        for(int i = 0; i < selected.size(); i++){
            items.add(selected.keyAt(i));
        }

        return items;
    }

    public int getSelectedItemCount(){
        return selected.size();
    }

    public boolean isSelected(int position){
        return getSelectedItems().contains(position);
    }

    public void clearSelection(){
        List<Integer> selection = getSelectedItems();
        selected.clear();
        for(Integer i : selection){
            notifyItemChanged(i);
        }
    }

    public void toggleSelection(int position){
        if(selected.get(position, false)){
            selected.delete(position);
        }else{
            selected.put(position, true);
        }

        notifyItemChanged(position);
    }

    public static class ImageGridRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,View.OnLongClickListener {
        public ImageView imageView;
        OnImageClickListener listener;
        public ImageGridRecyclerViewHolder(View v, OnImageClickListener listener) {
            super(v);
            imageView = v.findViewById(R.id.grid_image_view);

            this.listener = listener;
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.e("SecureGallery", "pos: " + getAdapterPosition());
            listener.onImageClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            listener.onImageLongClick(getAdapterPosition());

            return false;
        }
    }

    public interface OnImageClickListener{
        void onImageClick(int position);
        void onImageLongClick(int position);
    }
}